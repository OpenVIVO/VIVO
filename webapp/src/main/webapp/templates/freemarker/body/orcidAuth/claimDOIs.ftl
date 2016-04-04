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
    <br/>
<#else>
    <h1>Welcome Back ${profileName}</h1>
    <br/>
</#if>
<br />
<#if externalIds??>
    <p>OpenVIVO has found works for you to claim.  For each work, you will have an opportunity to indicate your role in the work.</p>
    <p>There are ${externalCount} work<#if externalCount != 1>s</#if> to claim.</p>
    <p>Click the "Claim these works" to claim the works.  Click Cancel if you do not wish to claim works at this time.</p>
    <form action="${urls.base}/createAndLink/doi" method="post">
        <p>There are ${externalCount} DOI<#if externalCount != 1>s</#if> to claim.</p>
        <input type="hidden" name="externalIds" value="${externalIds}" />
        <input type="hidden" name="action" value="findID" />
        <input type="hidden" name="profileUri" value="${profileUri!}" />
        <input type="submit" class="submit" value="Claim these works" /> <a href="${profileUri}">Cancel</a>
    </form>
<#else>
    <a href="${profileUri}">Go To My Profile</a>
</#if>
