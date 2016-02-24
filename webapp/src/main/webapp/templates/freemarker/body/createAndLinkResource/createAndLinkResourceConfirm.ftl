<style>
    .citation_claimed:before {
        content: url('../images/createAndLink/tick.png');
#        margin-left: 400px;
        zoom: 75%; margin-left: 535px;
        float: left;
        position: absolute;
    }
    .citation_claimed:hover:before {
        opacity: 0.2;
    }
    .citation_claimed .citation {
        opacity: 0.2;
    }
    .citation_claimed:hover .citation {
        opacity: 1.0;
    }
    .citation_type {
        font-style: italic;
    }
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
<#setting number_format="computer">
<#escape var as var?html>
    <form method="post">
        Is this your paper?<br />
        <br />
        <input type="hidden" name="externalId" value="${citation.externalId!}" />
        <!-- Output Citation -->
        <#if citation.alreadyClaimed>
            <div class="citation_claimed">
        </#if>
        <div class="citation">
            <#assign proposedAuthor=false />
            <#if citation.type?has_content>
                <#switch citation.type>
                    <#case "article">
                        <span class="citation_type">Article</span><br/><#break>
                    <#case "article-journal">
                        <span class="citation_type">Journal Article</span><br/><#break>
                    <#case "book">
                        <span class="citation_type">Book</span><br/><#break>
                    <#case "chapter">
                        <span class="citation_type">Chapter</span><br/><#break>
                    <#case "dataset">
                        <span class="citation_type">Dataset</span><br/><#break>
                    <#case "figure">
                        <span class="citation_type">Image</span><br/><#break>
                    <#case "graphic">
                        <span class="citation_type">Image</span><br/><#break>
                    <#case "legal_case">
                        <span class="citation_type">Legal Case</span><br/><#break>
                    <#case "legislation">
                        <span class="citation_type">Legislation</span><br/><#break>
                    <#case "manuscript">
                        <span class="citation_type">Manuscript</span><br/><#break>
                    <#case "map">
                        <span class="citation_type">Map</span><br/><#break>
                    <#case "musical_score">
                        <span class="citation_type">Muscial Score</span><br/><#break>
                    <#case "paper-conference">
                        <span class="citation_type">Conference Paper</span><br/><#break>
                    <#case "patent">
                        <span class="citation_type">Patent</span><br/><#break>
                    <#case "personal_communication">
                        <span class="citation_type">Letter</span><br/><#break>
                    <#case "post-weblog">
                        <span class="citation_type">Blog</span><br/><#break>
                    <#case "report">
                        <span class="citation_type">Report</span><br/><#break>
                    <#case "review">
                        <span class="citation_type">Review</span><br/><#break>
                    <#case "speech">
                        <span class="citation_type">Speech</span><br/><#break>
                    <#case "thesis">
                        <span class="citation_type">Thesis</span><br/><#break>
                    <#case "webpage">
                        <span class="citation_type">Webpage</span><br/><#break>
                </#switch>
            </#if>
            <span class="citation_title">${citation.title!}</span><br />
            <#if citation.authors??>
                <#list citation.authors as author>
                    <span class="citation_author">
                        <#if citation.alreadyClaimed>
                            <span>${author.name!}</span>
                        <#else>
                            <#if !author.linked>
                                <input type="radio" id="author${author?counter}" name="contributor${citation.externalId}" value="author${author?counter}" <#if author.proposed>checked</#if> class="radioWithLabel" />
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
            <span class="citation_journal">${citation.journal!}</span> ${citation.publicationYear!}; ${citation.volume!}(${citation.issue!}):${citation.pagination!}<br />
        </div>

        <#if citation.alreadyClaimed>
            <span class="claimed">You have already claimed this work.</span>
        <#else>
            <input type="radio" id="author" name="contributor${citation.externalId}" value="author" <#if !proposedAuthor>checked</#if> class="radioWithLabel" /><label for="author" class="labelForRadio"> Author</label><br />
            <input type="radio" id="editor" name="contributor${citation.externalId}" value="editor" class="radioWithLabel" /><label for="editor" class="labelForRadio"> Editor</label><br />
            <input type="radio" id="notmine" name="contributor${citation.externalId}" value="notmine" class="radioWithLabel" /><label for="notmine" class="labelForRadio"> This is not my work</label><br />
        </#if>
        <input type="hidden" name="externalResource${citation.externalId}" value="${externalResource!}" />
        <input type="hidden" name="externalProvider${citation.externalId}" value="${externalProvider!}" />
        <input type="hidden" name="vivoUri${citation.externalId}" value="${vivoUri!}" />
        <#if citation.alreadyClaimed>
            </div>
        </#if>
        <div style="clear: both;"></div>
        <!-- End Citation -->
        <div class="buttons">
            <input type="hidden" name="action" value="confirmID" />
            <input type="submit" value="Confirm" />
        </div>
    </form>
</#escape>
