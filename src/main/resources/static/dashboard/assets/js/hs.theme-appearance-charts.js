HSCore.components.HSChartJS.addTheme('updatingBarChart', 'dark', {
  data: {
    datasets: [{
      backgroundColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary,
      borderColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      backgroundColor: '#3d4145',
      borderColor: '#3d4145'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc'
        }
      }],
      xAxes: [{
        ticks: {
          fontColor: '#c5c8cc'
        }
      }]
    }
  }
})
HSCore.components.HSChartJS.addTheme('updatingBarChart', 'default', {
  data: {
    datasets: [{
      backgroundColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary,
      borderColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      backgroundColor: '#e7eaf3',
      borderColor: '#e7eaf3'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#e7eaf3',
          zeroLineColor: '#e7eaf3'
        }
      }],
      xAxes: [{
        ticks: {
          fontColor: '#97a4af'
        }
      }]
    }
  }
})

HSCore.components.HSChartJS.addTheme('updatingLineChart', 'dark', {
  data: {
    datasets: [{
      "backgroundColor": [window.hs_config.gulpRGBA(`${window.hs_config.themeAppearance.styles.colors.primary}, .3`), "rgba(255, 255, 255, 0)"]
    },
    {
      "backgroundColor": ["rgba(0, 201, 219, .3)", "rgba(255, 255, 255, 0)"]
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    }
  }
})

HSCore.components.HSChartJS.addTheme('ecommerce-sales', 'dark', {
  data: {
    datasets: [{
      backgroundColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary,
      borderColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      backgroundColor: '#3d4145',
      borderColor: '#3d4145'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    }
  }
})

HSCore.components.HSChartJS.addTheme('ecommerce-overview-1', 'dark', {
  data: {
    datasets: [{
      borderColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBorderColor: window.hs_config.themeAppearance.styles.colors.primary,
      pointBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      borderColor: '#3d4145',
      hoverBorderColor: '#3d4145',
      pointBackgroundColor: '#3d4145'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    },
    tooltips: {
      lineWithLineColor: "#2f3235"
    }
  }
})

HSCore.components.HSChartJS.addTheme('ecommerce-overview-2', 'dark', {
  data: {
    datasets: [{
      borderColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBorderColor: window.hs_config.themeAppearance.styles.colors.primary,
      pointBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      borderColor: '#3d4145',
      hoverBorderColor: '#3d4145',
      pointBackgroundColor: '#3d4145'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    },
    tooltips: {
      lineWithLineColor: "#2f3235"
    }
  }
})

HSCore.components.HSChartJS.addTheme('ecommerce-overview-3', 'dark', {
  data: {
    datasets: [{
      borderColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBorderColor: window.hs_config.themeAppearance.styles.colors.primary,
      pointBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      borderColor: '#3d4145',
      hoverBorderColor: '#3d4145',
      pointBackgroundColor: '#3d4145'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    },
    tooltips: {
      lineWithLineColor: "#2f3235"
    }
  }
})

HSCore.components.HSChartJS.addTheme('ecommerce-overview-4', 'dark', {
  data: {
    datasets: [{
      borderColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBorderColor: window.hs_config.themeAppearance.styles.colors.primary,
      pointBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      borderColor: '#3d4145',
      hoverBorderColor: '#3d4145',
      pointBackgroundColor: '#3d4145'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    },
    tooltips: {
      lineWithLineColor: "#2f3235"
    }
  }
})

HSCore.components.HSChartJS.addTheme('ecommerce-customer-details', 'dark', {
  data: {
    datasets: [{
      borderColor: window.hs_config.themeAppearance.styles.colors.primary,
      hoverBorderColor: window.hs_config.themeAppearance.styles.colors.primary,
      pointBackgroundColor: window.hs_config.themeAppearance.styles.colors.primary
    },
    {
      borderColor: '#3d4145',
      hoverBorderColor: '#3d4145',
      pointBackgroundColor: '#3d4145'
    }]
  },
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    }
  }
})

HSCore.components.HSChartJS.addTheme('project', 'dark', {
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    }
  }
})

HSCore.components.HSChartJS.addTheme('referrals', 'dark', {
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    }
  }
})

HSCore.components.HSChartJS.addTheme('line-chart-with-grid', 'dark', {
  options: {
    scales: {
      yAxes: [{
        gridLines: {
          color: '#2f3235',
          zeroLineColor: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }],
      xAxes: [{
        gridLines: {
          color: '#2f3235'
        },
        ticks: {
          fontColor: '#c5c8cc',
        }
      }]
    }
  }
})

if (HSCore.components.hasOwnProperty('HSChartMatrixJS')) {
  HSCore.components.HSChartMatrixJS.addTheme('chartjs-matrix', 'dark', {
    options: {
      scales: {
        xAxes: [{
          ticks: {
            "fontColor": '#c5c8cc'
          }
        }],
        yAxes: [{
          ticks: {
            "fontColor": '#c5c8cc'
          }
        }]
      }
    }
  })
}

HSCore.components.HSChartJS.addTheme('doughnutHalfChart', 'dark', {
  data: {
    datasets: [{
      borderColor: '#25282a',
      hoverBorderColor: '#25282a'
    }]
  }
})

HSCore.components.HSChartJS.addTheme('updatingDoughnutHalfChart', 'dark', {
  data: {
    datasets: [{
      borderColor: '#25282a',
      hoverBorderColor: '#25282a'
    }]
  }
})

HSCore.components.HSChartJS.addTheme('updatingDoughnutChart', 'dark', {
  data: {
    datasets: [{
      backgroundColor: [window.hs_config.themeAppearance.styles.colors.primary, '#00c9db', '#2f3235'],
      borderColor: '#25282a',
      hoverBorderColor: '#25282a'
    }]
  }
})

HSCore.components.HSChartJS.addTheme('doughnut-chart', 'dark', {
  data: {
    datasets: [{
      backgroundColor: [window.hs_config.themeAppearance.styles.colors.primary, '#00c9db', '#2f3235'],
      borderColor: '#25282a',
      hoverBorderColor: '#25282a'
    }]
  }
})

HSCore.components.HSChartJS.addTheme('strong-line-chart', 'dark', {
  options: {
    scales: {
      xAxes: [{
        ticks: {
          fontColor: '#c5c8cc'
        }
      }]
    }
  }
})
