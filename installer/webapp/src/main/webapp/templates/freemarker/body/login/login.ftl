<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Main template for the login page -->
<div float="left">
    <section id="login-orcid" class="">
        <h2>Log in</h2>
        <form role="form" id="login-orcid-form" action="${urls.base}/orcidAuth" method="get" name="login-orcid-form" _lpchecked="1">
            <div>
                <p>
                    OpenVIVO is available to anyone who has a registered ORCiD identifier.
                </p>
                <p class="submit"><input name="loginForm" class="green button" type="submit" value="Log in via ORCiD"></p>
            </div>
        </form>
    </section>
</div>
<div float="right">
    <@widget name="login" />
    <script>
        $('div.vivoAccount').show();
    </script>
</div>
