<style>
    .claim-for {
        float: right;
        border: 2px solid #3e8baa; /* #E0E0E0; */
        padding: 5px;
    }
    .claim-for h3 {
        text-align: center;
    }
</style>
<#if newAccount>
    <h1>Welcome ${profileName}</h1>
    <p>Need some text here.</p>
<#else>
    <h1>Welcome Back ${profileName}</h1>
    <p>We have found unclaimed works on your ORCID profile.</p>
</#if>
<#if externalIds??>
    <form action="${urls.base}/createAndLink/doi" method="post">
        <p>There are ${externalCount} DOI<#if externalCount != 1>s</#if> to claim.</p>
        <input type="hidden" name="externalIds" value="${externalIds}" />
        <input type="hidden" name="action" value="findID" />
        <input type="hidden" name="profileUri" value="${profileUri!}" />
        <input type="submit" class="submit" /><br />
        <a href="${profileUri}">Do not claim any works at this time</a>
    </form>
<#else>
    <a href="${profileUri}">Go To My Profile</a>
</#if>
