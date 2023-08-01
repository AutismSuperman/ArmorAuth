<!-- 通用 CSS -->
<#macro  common_header  title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta content="IE=edge" http-equiv="X-UA-Compatible">
    <meta content="" name="keywords">
    <meta content="" name="description">
    <title text="${title}"></title>
    <link rel="stylesheet" href="/static/bootstrap/css/bootstrap.min.css">
</#macro>


<#macro  common_javascript >
    <script src="/static/bootstrap/js/bootstrap.bundle.min.js"></script>
    <script src="/static/oauth/js/bootstrap.alter.js"></script>
    <!-- 开发环境版本，包含了有帮助的命令行警告 -->
    <script src="/static/oauth/jquery/jquery.min.js"></script>
</#macro>

