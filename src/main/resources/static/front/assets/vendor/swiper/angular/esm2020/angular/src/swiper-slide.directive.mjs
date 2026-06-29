import { Directive, Input } from '@angular/core';
import { coerceBooleanProperty } from './utils/utils';
import * as i0 from "@angular/core";
export class SwiperSlideDirective {
    constructor(template) {
        this.template = template;
        this.class = '';
        this.autoplayDelay = null;
        this.slideData = {
            isActive: false,
            isPrev: false,
            isNext: false,
            isVisible: false,
            isDuplicate: false,
        };
    }
    set ngClass(val) {
        this.class = [this.class || '', val].join(' ');
    }
    set zoom(val) {
        this._zoom = coerceBooleanProperty(val);
    }
    get zoom() {
        return this._zoom;
    }
    get classNames() {
        return this._classNames;
    }
    set classNames(val) {
        if (this._classNames === val) {
            return;
        }
        this._classNames = val;
        this.slideData = {
            isActive: this._hasClass(['swiper-slide-active', 'swiper-slide-duplicate-active']),
            isVisible: this._hasClass(['swiper-slide-visible']),
            isDuplicate: this._hasClass(['swiper-slide-duplicate']),
            isPrev: this._hasClass(['swiper-slide-prev', 'swiper-slide-duplicate-prev']),
            isNext: this._hasClass(['swiper-slide-next', 'swiper-slide-duplicate-next']),
        };
    }
    _hasClass(classNames) {
        return classNames.some((className) => this._classNames.indexOf(className) >= 0);
    }
}
SwiperSlideDirective.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "13.3.11", ngImport: i0, type: SwiperSlideDirective, deps: [{ token: i0.TemplateRef }], target: i0.ɵɵFactoryTarget.Directive });
SwiperSlideDirective.ɵdir = i0.ɵɵngDeclareDirective({ minVersion: "12.0.0", version: "13.3.11", type: SwiperSlideDirective, selector: "ng-template[swiperSlide]", inputs: { virtualIndex: "virtualIndex", class: "class", ngClass: "ngClass", autoplayDelay: ["data-swiper-autoplay", "autoplayDelay"], zoom: "zoom" }, ngImport: i0 });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "13.3.11", ngImport: i0, type: SwiperSlideDirective, decorators: [{
            type: Directive,
            args: [{
                    selector: 'ng-template[swiperSlide]',
                }]
        }], ctorParameters: function () { return [{ type: i0.TemplateRef }]; }, propDecorators: { virtualIndex: [{
                type: Input
            }], class: [{
                type: Input
            }], ngClass: [{
                type: Input
            }], autoplayDelay: [{
                type: Input,
                args: ['data-swiper-autoplay']
            }], zoom: [{
                type: Input
            }] } });
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoic3dpcGVyLXNsaWRlLmRpcmVjdGl2ZS5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbIi4uLy4uLy4uLy4uLy4uL3NyYy9hbmd1bGFyL3NyYy9zd2lwZXItc2xpZGUuZGlyZWN0aXZlLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBLE9BQU8sRUFBRSxTQUFTLEVBQUUsS0FBSyxFQUFlLE1BQU0sZUFBZSxDQUFDO0FBQzlELE9BQU8sRUFBRSxxQkFBcUIsRUFBRSxNQUFNLGVBQWUsQ0FBQzs7QUFJdEQsTUFBTSxPQUFPLG9CQUFvQjtJQThDL0IsWUFBbUIsUUFBMEI7UUFBMUIsYUFBUSxHQUFSLFFBQVEsQ0FBa0I7UUE1Q3BDLFVBQUssR0FBVyxFQUFFLENBQUM7UUFLRyxrQkFBYSxHQUFrQixJQUFJLENBQUM7UUE4Qm5FLGNBQVMsR0FBRztZQUNWLFFBQVEsRUFBRSxLQUFLO1lBQ2YsTUFBTSxFQUFFLEtBQUs7WUFDYixNQUFNLEVBQUUsS0FBSztZQUNiLFNBQVMsRUFBRSxLQUFLO1lBQ2hCLFdBQVcsRUFBRSxLQUFLO1NBQ25CLENBQUM7SUFHOEMsQ0FBQztJQTNDakQsSUFDSSxPQUFPLENBQUMsR0FBVztRQUNyQixJQUFJLENBQUMsS0FBSyxHQUFHLENBQUMsSUFBSSxDQUFDLEtBQUssSUFBSSxFQUFFLEVBQUUsR0FBRyxDQUFDLENBQUMsSUFBSSxDQUFDLEdBQUcsQ0FBQyxDQUFDO0lBQ2pELENBQUM7SUFFRCxJQUNJLElBQUksQ0FBQyxHQUFZO1FBQ25CLElBQUksQ0FBQyxLQUFLLEdBQUcscUJBQXFCLENBQUMsR0FBRyxDQUFDLENBQUM7SUFDMUMsQ0FBQztJQUNELElBQUksSUFBSTtRQUNOLE9BQU8sSUFBSSxDQUFDLEtBQUssQ0FBQztJQUNwQixDQUFDO0lBR0QsSUFBSSxVQUFVO1FBQ1osT0FBTyxJQUFJLENBQUMsV0FBVyxDQUFDO0lBQzFCLENBQUM7SUFDRCxJQUFJLFVBQVUsQ0FBQyxHQUFHO1FBQ2hCLElBQUksSUFBSSxDQUFDLFdBQVcsS0FBSyxHQUFHLEVBQUU7WUFDNUIsT0FBTztTQUNSO1FBQ0QsSUFBSSxDQUFDLFdBQVcsR0FBRyxHQUFHLENBQUM7UUFDdkIsSUFBSSxDQUFDLFNBQVMsR0FBRztZQUNmLFFBQVEsRUFBRSxJQUFJLENBQUMsU0FBUyxDQUFDLENBQUMscUJBQXFCLEVBQUUsK0JBQStCLENBQUMsQ0FBQztZQUNsRixTQUFTLEVBQUUsSUFBSSxDQUFDLFNBQVMsQ0FBQyxDQUFDLHNCQUFzQixDQUFDLENBQUM7WUFDbkQsV0FBVyxFQUFFLElBQUksQ0FBQyxTQUFTLENBQUMsQ0FBQyx3QkFBd0IsQ0FBQyxDQUFDO1lBQ3ZELE1BQU0sRUFBRSxJQUFJLENBQUMsU0FBUyxDQUFDLENBQUMsbUJBQW1CLEVBQUUsNkJBQTZCLENBQUMsQ0FBQztZQUM1RSxNQUFNLEVBQUUsSUFBSSxDQUFDLFNBQVMsQ0FBQyxDQUFDLG1CQUFtQixFQUFFLDZCQUE2QixDQUFDLENBQUM7U0FDN0UsQ0FBQztJQUNKLENBQUM7SUFFTyxTQUFTLENBQUMsVUFBb0I7UUFDcEMsT0FBTyxVQUFVLENBQUMsSUFBSSxDQUFDLENBQUMsU0FBUyxFQUFFLEVBQUUsQ0FBQyxJQUFJLENBQUMsV0FBVyxDQUFDLE9BQU8sQ0FBQyxTQUFTLENBQUMsSUFBSSxDQUFDLENBQUMsQ0FBQztJQUNsRixDQUFDOztrSEFwQ1Usb0JBQW9CO3NHQUFwQixvQkFBb0I7NEZBQXBCLG9CQUFvQjtrQkFIaEMsU0FBUzttQkFBQztvQkFDVCxRQUFRLEVBQUUsMEJBQTBCO2lCQUNyQztrR0FFVSxZQUFZO3NCQUFwQixLQUFLO2dCQUNHLEtBQUs7c0JBQWIsS0FBSztnQkFFRixPQUFPO3NCQURWLEtBQUs7Z0JBSXlCLGFBQWE7c0JBQTNDLEtBQUs7dUJBQUMsc0JBQXNCO2dCQUV6QixJQUFJO3NCQURQLEtBQUsiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgeyBEaXJlY3RpdmUsIElucHV0LCBUZW1wbGF0ZVJlZiB9IGZyb20gJ0Bhbmd1bGFyL2NvcmUnO1xyXG5pbXBvcnQgeyBjb2VyY2VCb29sZWFuUHJvcGVydHkgfSBmcm9tICcuL3V0aWxzL3V0aWxzJztcclxuQERpcmVjdGl2ZSh7XHJcbiAgc2VsZWN0b3I6ICduZy10ZW1wbGF0ZVtzd2lwZXJTbGlkZV0nLFxyXG59KVxyXG5leHBvcnQgY2xhc3MgU3dpcGVyU2xpZGVEaXJlY3RpdmUge1xyXG4gIEBJbnB1dCgpIHZpcnR1YWxJbmRleDogbnVtYmVyO1xyXG4gIEBJbnB1dCgpIGNsYXNzOiBzdHJpbmcgPSAnJztcclxuICBASW5wdXQoKVxyXG4gIHNldCBuZ0NsYXNzKHZhbDogc3RyaW5nKSB7XHJcbiAgICB0aGlzLmNsYXNzID0gW3RoaXMuY2xhc3MgfHwgJycsIHZhbF0uam9pbignICcpO1xyXG4gIH1cclxuICBASW5wdXQoJ2RhdGEtc3dpcGVyLWF1dG9wbGF5JykgYXV0b3BsYXlEZWxheTogc3RyaW5nIHwgbnVsbCA9IG51bGw7XHJcbiAgQElucHV0KClcclxuICBzZXQgem9vbSh2YWw6IGJvb2xlYW4pIHtcclxuICAgIHRoaXMuX3pvb20gPSBjb2VyY2VCb29sZWFuUHJvcGVydHkodmFsKTtcclxuICB9XHJcbiAgZ2V0IHpvb20oKSB7XHJcbiAgICByZXR1cm4gdGhpcy5fem9vbTtcclxuICB9XHJcbiAgcHJpdmF0ZSBfem9vbTogYm9vbGVhbjtcclxuICBzbGlkZUluZGV4OiBudW1iZXI7XHJcbiAgZ2V0IGNsYXNzTmFtZXMoKSB7XHJcbiAgICByZXR1cm4gdGhpcy5fY2xhc3NOYW1lcztcclxuICB9XHJcbiAgc2V0IGNsYXNzTmFtZXModmFsKSB7XHJcbiAgICBpZiAodGhpcy5fY2xhc3NOYW1lcyA9PT0gdmFsKSB7XHJcbiAgICAgIHJldHVybjtcclxuICAgIH1cclxuICAgIHRoaXMuX2NsYXNzTmFtZXMgPSB2YWw7XHJcbiAgICB0aGlzLnNsaWRlRGF0YSA9IHtcclxuICAgICAgaXNBY3RpdmU6IHRoaXMuX2hhc0NsYXNzKFsnc3dpcGVyLXNsaWRlLWFjdGl2ZScsICdzd2lwZXItc2xpZGUtZHVwbGljYXRlLWFjdGl2ZSddKSxcclxuICAgICAgaXNWaXNpYmxlOiB0aGlzLl9oYXNDbGFzcyhbJ3N3aXBlci1zbGlkZS12aXNpYmxlJ10pLFxyXG4gICAgICBpc0R1cGxpY2F0ZTogdGhpcy5faGFzQ2xhc3MoWydzd2lwZXItc2xpZGUtZHVwbGljYXRlJ10pLFxyXG4gICAgICBpc1ByZXY6IHRoaXMuX2hhc0NsYXNzKFsnc3dpcGVyLXNsaWRlLXByZXYnLCAnc3dpcGVyLXNsaWRlLWR1cGxpY2F0ZS1wcmV2J10pLFxyXG4gICAgICBpc05leHQ6IHRoaXMuX2hhc0NsYXNzKFsnc3dpcGVyLXNsaWRlLW5leHQnLCAnc3dpcGVyLXNsaWRlLWR1cGxpY2F0ZS1uZXh0J10pLFxyXG4gICAgfTtcclxuICB9XHJcblxyXG4gIHByaXZhdGUgX2hhc0NsYXNzKGNsYXNzTmFtZXM6IHN0cmluZ1tdKSB7XHJcbiAgICByZXR1cm4gY2xhc3NOYW1lcy5zb21lKChjbGFzc05hbWUpID0+IHRoaXMuX2NsYXNzTmFtZXMuaW5kZXhPZihjbGFzc05hbWUpID49IDApO1xyXG4gIH1cclxuICBzbGlkZURhdGEgPSB7XHJcbiAgICBpc0FjdGl2ZTogZmFsc2UsXHJcbiAgICBpc1ByZXY6IGZhbHNlLFxyXG4gICAgaXNOZXh0OiBmYWxzZSxcclxuICAgIGlzVmlzaWJsZTogZmFsc2UsXHJcbiAgICBpc0R1cGxpY2F0ZTogZmFsc2UsXHJcbiAgfTtcclxuXHJcbiAgcHJpdmF0ZSBfY2xhc3NOYW1lczogc3RyaW5nO1xyXG4gIGNvbnN0cnVjdG9yKHB1YmxpYyB0ZW1wbGF0ZTogVGVtcGxhdGVSZWY8YW55Pikge31cclxufVxyXG4iXX0=