let session_error_alter = $('#liveAlertPlaceholder');

var countdown = 60;

function setTime(obj) {
    if (countdown == 0) {
        obj.attr('disabled', false);
        //obj.removeattr("disabled");
        obj.html("获取验证码");
        countdown = 10;
        return;
    } else {
        obj.attr('disabled', true);
        obj.html("重新发送(" + countdown + ")");
        countdown--;
    }
    setTimeout(function () {
            setTime(obj)
        }
        , 1000)
}


$("#send_code").click(function () {
    setTime($(this))
})

$("body").keydown(function (event) {
    if (event.keyCode === 13) {//keyCode=13是回车键
        let up = $('#up');
        let pc = $('#pc');
        let up_display = up.css('display');
        let pc_display = pc.css('display');
        if (pc_display === 'none') {
            $('#up_login').click();
        } else {
            $('#pc_login').click();
        }
    }
})
$('.session_toggle-button').click(function (event) {
    event.preventDefault();
    let up = $('#up');
    let pc = $('#pc');
    let up_display = up.css('display');
    let pc_display = pc.css('display');
    if (pc_display === 'none') {
        up.hide();
        pc.show();
        session_error_alter.empty();
    } else {
        up.show();
        pc.hide();
        session_error_alter.empty();
    }
});
$('#up_login').click(function () {
    $.ajax({
        url: '/login',
        data: {
            username: $('#username').val(),
            password: $('#password').val()
        },
        type: "post",
        dataType: 'json',
        success: function (result) {
            location.href = result.redirectUrl;
        },
        error: function (xhr, status, error) {
            if (xhr.responseJSON) {
                var errorData = xhr.responseJSON;
                alterUtil.alert(errorData.msg, 'danger', session_error_alter)
            } else {
                var errorText = xhr.responseText;
                var errorData = JSON.parse(errorText);
                alterUtil.alert(errorData.msg, 'danger', session_error_alter)
            }
        }
    });
});
$('#pc_login').click(function () {
    $.ajax({
        url: '/login/captcha',
        data: {
            account: $('#account').val(),
            captcha: $('#captcha').val()
        },
        type: "post",
        dataType: 'json',
        success: function (result) {
            location.href = result.redirectUrl;
        },
        error: function (xhr, status, error) {
            if (xhr.responseJSON) {
                var errorData = xhr.responseJSON;
                alterUtil.alert(errorData.msg, 'danger', session_error_alter)
            } else {
                var errorText = xhr.responseText;
                var errorData = JSON.parse(errorText);
                alterUtil.alert(errorData.msg, 'danger', session_error_alter)
            }
        }
    });
});