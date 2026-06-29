package com.kuocai.cdn.controller.api.v1;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.kuocai.cdn.common.mongo.entity.FlowBillingLogic;
import com.kuocai.cdn.service.FlowBillingService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiDateUtil;
import com.kuocai.cdn.vo.rest.FlowBillingTokenVo;
import com.kuocai.cdn.vo.rest.FlowBillingVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/billing")
public class BillingController {

    private final FlowBillingService flowBillingService;
    private final MongoTemplate mongoTemplate;
    private final String billingToken;

    BillingController(FlowBillingService flowBillingService, MongoTemplate mongoTemplate, @Value("${api.billing.token:}") String billingToken) {
        this.flowBillingService = flowBillingService;
        this.mongoTemplate = mongoTemplate;
        this.billingToken = billingToken;
    }

    @PostMapping(value = "waiting-list")
    public ResponseEntity<String> index(@RequestBody FlowBillingTokenVo vo) {
        if (!isValidToken(vo.getToken())) {
            return ResponseEntity.ok("AUTH");
        }
        // 获取当前时间一小时前的整点时间
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(1);
        // yyyy-MM-dd HH:mm:ss
        String time = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00:00"));
        // {time: {$lt: "2024-07-01 20:00:00"}, promise: "pending"}
        Criteria criteria = Criteria.where("time").lt(time);
        Query query = Query.query(Criteria.where("promise").is("pending").andOperator(criteria));
        List<FlowBillingLogic> flowBillingLogics = mongoTemplate.find(query, FlowBillingLogic.class);
        if (Assert.isEmpty(flowBillingLogics)) {
            return ResponseEntity.ok("NOT FOUND");
        }
        String result = JSONArray.toJSONString(flowBillingLogics);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "flow")
    public ResponseEntity<String> flow(@RequestBody FlowBillingVo vo) {
        Query query = Query.query(Criteria.where("_id").is(vo.getOid()));
        FlowBillingLogic flowBillingLogic = mongoTemplate.findOne(query, FlowBillingLogic.class);
        if (!isValidToken(vo.getToken())) {
            return ResponseEntity.ok("AUTH");
        }
        if (Assert.isEmpty(flowBillingLogic)) {
            return ResponseEntity.ok("NOT FOUND");
        }
        if ("resolved".equals(flowBillingLogic.getPromise())) {
            return ResponseEntity.ok("RESOLVED");
        }
        try {
            DateTime start = DateUtil.parse(flowBillingLogic.getTime(), "yyyy-MM-dd HH:mm:ss");
            DateTime end = DateUtil.offsetHour(start, -1);
            Long total = Long.valueOf(vo.getTotal());
            Long uid = Long.valueOf(vo.getUid());
            flowBillingLogic.setSummary(total);
            flowBillingLogic.setPromise("resolved");
            flowBillingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
            flowBillingService.flowStatement(new ArrayList<>(), total, uid, start, end);
            mongoTemplate.save(flowBillingLogic);
        } catch (Exception e) {
            log.trace("ERROR", e);
            return ResponseEntity.ok(String.format("ERROR %s", e.getMessage()));
        }
        return ResponseEntity.ok("OK");
    }

    private boolean isValidToken(String token) {
        return Assert.notEmpty(billingToken) && billingToken.equals(token);
    }

}
