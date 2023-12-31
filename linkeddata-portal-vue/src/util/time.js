import tool from './tool'

var Time = {
	//获取当前时间戳
	getUnix: function() {
		var date = new Date();
		return date.getTime();
	},
	//获取今天0点0分0秒的时间戳
	getTodayUnix: function() {
		var date = new Date();
		date.setHours(0);
		date.setMinutes(0);
		date.setSeconds(0);
		date.setMilliseconds(0);
		return date.getTime();
	},
	//获取今年1月1日0点0秒的时间戳
	getYearUnix: function() {
		var date = new Date();
		date.setMonth(0);
		date.setDate(1);
		date.setHours(0);
		date.setMinutes(0);
		date.setSeconds(0);
		date.setMilliseconds(0);
		return date.getTime();
	},
	//获取标准年月日
	getLastDate: function(time) {
		var date = new Date(time);
		var month = date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1;
		var day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
		return date.getFullYear() + '-' + month + '-' + day;
	},
	dateFormat: function (date, fmt='yyyy-MM-dd hh:mm:ss') {
		date = new Date(date)
		var o = {
			"M+" : date.getMonth()+1,                 //月份
			"d+" : date.getDate(),                    //日
			"h+" : date.getHours(),                   //小时
			"m+" : date.getMinutes(),                 //分
			"s+" : date.getSeconds(),                 //秒
			"q+" : Math.floor((date.getMonth()+3)/3), //季度
			"S"  : date.getMilliseconds()             //毫秒
		};
		if(/(y+)/.test(fmt)) {
			fmt=fmt.replace(RegExp.$1, (date.getFullYear()+"").substr(4 - RegExp.$1.length));
		}
		for(var k in o) {
			if(new RegExp("("+ k +")").test(fmt)){
				fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
			}
		}
		return fmt;
	},
	//转换时间
	getFormateTime: function(timestamp) {
		timestamp = new Date(timestamp)
		var now = this.getUnix();
		var today = this.getTodayUnix();
		//var year = this.getYearUnix();
		var timer = (now - timestamp) / 1000;
		var tip = '';

		if (timer <= 0) {
			tip = '刚刚';
		} else if (Math.floor(timer / 60) <= 0) {
			tip = '刚刚';
		} else if (timer < 3600) {
			tip = Math.floor(timer / 60) + '分钟前';
		} else if (timer >= 3600 && (timestamp - today >= 0)) {
			tip = Math.floor(timer / 3600) + '小时前';
		} else if (timer / 86400 <= 31) {
			tip = Math.ceil(timer / 86400) + '天前';
		} else {
			tip = this.getLastDate(timestamp);
		}
		return tip;
	}
}

export default {
	mounted(el, binding) {
		const { value, modifiers} = binding
		if (modifiers.tip) {
			el.innerHTML = Time.getFormateTime(value)
			el.__timeout__ = setInterval(() => {
				el.innerHTML = Time.getFormateTime(value)
			}, 60000)
		} else {
			const format = el.getAttribute('format') || undefined
			el.innerHTML = tool.dateFormat(value, format)
		}
	}
};
