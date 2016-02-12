<style>
    .citation_title {
        font-weight: bold;
    }
    .citation_journal {
        font-style: italic;
    }
    .claimed {
        font-weight: bold;
    }
    .linked {
        font-style: italic;
    }
    label {
        display: inline;
    }
    .radioWithLabel:checked + .labelForRadio {
        font-weight: bold;
    }
</style>
<#escape var as var?html>
    <form method="post">
        Is this your paper?<br />
        <br />
        <input type="hidden" name="doi" value="${citation.DOI!}" />
        <!-- Output Citation -->
        <#assign proposedAuthor=false />
        <span class="citation_title">${citation.title!}</span><br />
        <#if citation.authors??>
            <#list citation.authors as author>
                <span class="citation_author">
                    <#if citation.alreadyClaimed>
                        <span>${author.name!}</span>
                    <#else>
                        <#if !author.linked>
                            <input type="radio" id="author${author?counter}" name="contributor${citation.DOI}" value="author${author?counter}" <#if author.proposed>checked</#if> class="radioWithLabel" />
                            <label for="author${author?counter}" class="labelForRadio">${author.name!}</label>
                            <#if author.proposed><#assign proposedAuthor=true /></#if>
                        <#else>
                            <span class="linked">${author.name!}</span>
                        </#if>
                    </#if>
                </span>
                <#sep>; </#sep>
            </#list><br />
        </#if>
        <span class="citation_journal">${citation.journal!}</span> ${citation.publicationYear!?c}; ${citation.volume!}(${citation.issue!}):${citation.pagination!}<br />

        <#if citation.alreadyClaimed>
            <span class="claimed">You have already claimed this work.</span>
        <#else>
            <input type="radio" id="author" name="contributor${citation.DOI}" value="author" <#if !proposedAuthor>checked</#if> class="radioWithLabel" /><label for="author" class="labelForRadio"> Author</label><br />
            <input type="radio" id="editor" name="contributor${citation.DOI}" value="editor" class="radioWithLabel" /><label for="editor" class="labelForRadio"> Editor</label><br />
            <input type="radio" id="notmine" name="contributor${citation.DOI}" value="notmine" class="radioWithLabel" /><label for="notmine" class="labelForRadio"> This is not my work</label><br />
        </#if>
        <input type="hidden" name="json${citation.DOI}"    value="${json!}" />
        <input type="hidden" name="vivoUri${citation.DOI}" value="${vivoUri!}" />
        <!-- End Citation -->

        <input type="hidden" name="action" value="confirmDOI" />
        <input type="submit" value="Confirm" />
    </form>
</#escape>
