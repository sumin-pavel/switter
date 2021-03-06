<#import "parts/common.ftl" as common>
<#import "parts/login.ftl" as loginForm>

<@common.page>
<#if Session?? && Session.SPRING_SECURITY_LAST_EXCEPTION??>
<div class="alert alert-danger" role="alert">
    ${Session.SPRING_SECURITY_LAST_EXCEPTION.message}
</div>
</#if>
<@loginForm.login "/login" false/>
</@common.page>