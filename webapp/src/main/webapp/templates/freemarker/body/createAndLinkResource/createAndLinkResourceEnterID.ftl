<form method="post">
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
        <#break>
</#switch>
    <textarea name="externalIds" rows="15" cols="50"></textarea><br />
    <input type="submit" class="submit" /><br />
    <input type="hidden" name="action" value="findID" />
</form>
