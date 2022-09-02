$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	//当用户点击发布后，将当前的服务框隐藏掉
	$("#publishModal").modal("hide");
	//当用户点击发布后，不能直接显示成功与否的提示框，而是要根据服务器传回来的信息
	//进行判断，这个时候我们就要发送异步请求
// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求(POST)
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title,"content":content},
		function(data) {
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