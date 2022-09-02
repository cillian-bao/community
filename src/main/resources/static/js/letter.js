$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");
	//在这里进行服务端数据的发送
	var toName=$("recipient-name").val();
	var content=$("message-text").val();
	// 异步的发post请求
	$.post(
		CONTEXT_PATH + "/letter/send",
		{"toName":toName,"content":content},
		//处理服务端返回的结果
		function(data) {
			//转换回js对象
			data = $.parseJSON(data);
			// 在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2秒后,自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 刷新页面
				if(data.code == 0) {
					window.location.reload();
				}
			}, 2000);
		}
	);
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}