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
        padding: 5px;
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
    .entryId {
        background-color: #3e8baa; /* #E0E0E0; */
        color: #ffffff;
        padding: 5px;
        font-weight: bold;
        display: inline-block;
    }
    .entry {
        border: 2px solid #3e8baa; /* #E0E0E0; */
        padding: 5px;
    }
    label {
        display: inline;
    }
    .radioWithLabel:checked + .labelForRadio {
        font-weight: bold;
    }
    .description {
        padding-left: 22px;
    }
    .remainder {
        font-style: italic;
    }
    .claim-for {
        float: right;
        border: 2px solid #3e8baa; /* #E0E0E0; */
        padding: 5px;
    }
    .claim-for h3 {
        text-align: center;
    }
</style>
<#setting number_format="computer">
<#escape var as var?html>
<#include "createAndLinkResourceContributorRoles.ftl" />
<form method="post">
    <#if personLabel??>
        <div class="claim-for">
            <h3>Claiming works for<br />${personLabel}</h3>
            <#if personThumbUrl??>
                <img src="${urls.base}${personThumbUrl}" />
            </#if>
        </div>
    </#if>
    <h2>Confirm your work(s)</h2>
    Please check that these are the work(s) that you wish to claim, and indicate your relationship with them.<br /><br />
    <h4>Authors</h4>
    <div class="description">If you are an author of a work, please select your name in the author list.<br />
        Retrieved metadata may be incomplete. If you can not see your name listed, select "Unlisted Author".</div><br />
    <h4>Editors</h4>
    <div class="description">If you edited the work, please select "Editor".</div><br /><br />
    If you do not wish to claim a work, select "This is not my work".<br /><br />
    <#list citations as citation>
        <div class="entryId">
            <#if citation.externalProvider??>
            ${citation.externalProvider?upper_case}: ${citation.externalId}
            <#else>
                ID: ${citation.externalId}
            </#if>
        </div>
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
        <div class="entry">
            <!-- Output Citation -->
            <#if citation.alreadyClaimed>
            <div class="citation_claimed">
            </#if>
            <input type="hidden" name="externalId" value="${citation.externalId!}" />
            <div class="citation">
                <#assign proposedAuthor=false />
                <#if citation.title??><span class="citation_title">${citation.title}</span><br /></#if>
                <#assign formatted_citation>
                    <#if citation.journal??><span class="citation_journal">${citation.journal}</span></#if>
                    <#if citation.publicationYear??><span class="citation_year">${citation.publicationYear!};</span></#if>
                    <#if citation.volume??><span class="citation_volume">${citation.volume!}</#if>
                    <#if citation.issue??><span class="citation_issue">(${citation.issue!})</#if>
                    <#if citation.pagination??><span class="citation_pages">:${citation.pagination!}</#if>
                </#assign>
                <#if formatted_citation??>
                    <#noescape>${formatted_citation}</#noescape><br />
                </#if>
                <#if citation.authors??>
                    <#list citation.authors as author>
                        <span class="citation_author">
                            <#if citation.alreadyClaimed>
                                <span>${author.name!}</span>
                            <#else>
                                <#if !author.linked>
                                    <input type="radio" id="author${citation.externalId}-${author?counter}" name="contributor${citation.externalId}" value="author${author?counter}" <#if author.proposed>checked</#if> class="radioWithLabel" />
										<label for="author${citation.externalId}-${author?counter}" class="labelForRadio">${author.name!}</label>
                                    <#if author.proposed><#assign proposedAuthor=true /></#if>
                                <#else>
                                    <span class="linked">${author.name!}</span>
                                </#if>
                            </#if>
							</span>
                        <#sep>; </#sep>
                    </#list><br />
                </#if>
            </div>

            <#if citation.alreadyClaimed>
                <span class="claimed">You have already claimed this work.</span>
            <#else>
                <input type="radio" id="author${citation.externalId}" name="contributor${citation.externalId}" value="author" <#if !proposedAuthor>checked</#if> class="radioWithLabel" /><label for="author${citation.externalId}" class="labelForRadio"> Unlisted Author</label><br />
                <input type="radio" id="editor${citation.externalId}" name="contributor${citation.externalId}" value="editor" class="radioWithLabel" /><label for="editor${citation.externalId}" class="labelForRadio"> Editor</label><br />
                <@showContributorRoles citation=citation position=citation?counter />
                <input type="radio" id="notmine${citation.externalId}" name="contributor${citation.externalId}" value="notmine" class="radioWithLabel" /><label for="notmine${citation.externalId}" class="labelForRadio"> This is not my work</label><br />
            </#if>
            <input type="hidden" name="externalResource${citation.externalId}" value="${citation.externalResource!}" />
            <input type="hidden" name="externalProvider${citation.externalId}" value="${citation.externalProvider!}" />
            <input type="hidden" name="vivoUri${citation.externalId}" value="${citation.vivoUri!}" />
            <input type="hidden" name="profileUri" value="${profileUri!}" />
            <#if citation.alreadyClaimed>
                </div>
            </#if>
            <div style="clear: both;"></div>
        </div>
        <br/>
        <!-- End Citation -->
    </#list>
    <#if remainderIds??>
        <input type="hidden" name="remainderIds" value="${remainderIds}" />
    </#if>
    <div class="buttons">
        <input type="hidden" name="action" value="confirmID" />
        <input type="submit" value="Confirm" class="submit" />
        <#if remainderCount??>
            <span class="remainder">There are ${remainderCount} ids remaining</span>
        </#if>
    </div>
</form>
</#escape>
