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
<form method="post">
    <#if personLabel??>
        <div class="claim-for">
            <h3>Claiming works for<br />${personLabel}</h3>
            <#if personThumbUrl??>
                <img src="${urls.base}${personThumbUrl}" />
            </#if>
        </div>
    </#if>
    <#if showConfirmation??>
        <h2>Thank you</h2>
        There are no more works left to claim.<br />
        You may enter more IDs below, or view your profile.<br /><br />
        <#if profileUri??>
            <a href="${profileUrl(profileUri)}">Go to profile</a><br /><br />
        </#if>
    </#if>
    <#switch provider>
        <#case "doi">
            <h2>${i18n().create_and_link_enter(label)}</h2>
            You may enter one or more DOIs to match, and can be entered either as an ID or URL:<br /><br />
            e.g.<br />
            <i>ID</i>:  10.1038/nature01234<br />
            <i>URL</i>: http://dx.doi.org/10.1038/nature01234<br />
            <br />
            Currently, DOIs issued by Crossref, DataCite and mEDRA are supported.<br />
            Each DOI should be separated by a comma, semi colon or new line.<br /><br />
            <#break>
        <#case "pmid">
            <h2>${i18n().create_and_link_enter(label)}</h2>
            You may enter one or more PubMed IDs to match. Each ID should be separated by a comma, semi colon or new line.<br /><br />
            Note that metadata will be retrieved from Crossref, if the PubMed ID can be resolved to a DOI.<br /><br />
            <#break>
    </#switch>
    <textarea name="externalIds" rows="15" cols="50"></textarea><br />
    <input type="submit" class="submit" /><br />
    <input type="hidden" name="action" value="findID" />
    <input type="hidden" name="profileUri" value="${profileUri!}" />
</form>
