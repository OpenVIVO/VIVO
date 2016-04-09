<style>
    #rolesSelectionsGroupShow, #rolesSelectionsGroupHide {
        text-decoration: underline;
    }
    #rolesSelectionsGroupShow:hover, #rolesSelectionsGroupHide:hover {
        text-decoration: none;
    }
    .role_group_title {
        font-style: italic;
        font-size: 0.9em;
    }
    .role_group {
        margin-left: 1em;
    }
    .checkboxWithLabel:checked + .labelForCheckbox {
        font-weight: bold;
    }
    .contribution_title {
        padding-top: 1em;
        font-style: italic;
        font-weight: bold;
    }
    span.nowrap {
        white-space: nowrap;
    }
    ul.roles {
        -moz-column-count: 3;
        -moz-column-gap: 20px;
        -webkit-column-count: 3;
        -webkit-column-gap: 20px;
        column-count: 3;
        column-gap: 20px;
        font-size: 0.85em;
    }
</style>
<#macro contributorRole citationId roleId roleLabel>
<li><span class="nowrap">
    <input type="checkbox" id="${roleId}${citationId}" name="role${citationId}" value="${roleId}" class="checkboxWithLabel" />
    <label for="${roleId}${citationId}" class="labelForCheckbox"> ${roleLabel}</label>
</span></li>
</#macro>
<#macro showContributorRoles citation position>
    <script type="text/javascript">
        $(document).ready(function() {
            $('#rolesSelectionsGroup${position}').hide();
            $('#rolesSelectionsGroupShowButton${position}').show();

            $('#rolesSelectionsGroupShowButton${position}').click(function() {
                $('#rolesSelectionsGroupShow${position}').hide();
                $('#rolesSelectionsGroupHideButton${position}').show();
                $('#rolesSelectionsGroup${position}').show();
                return false;
            });
            $('#rolesSelectionsGroupHideButton${position}').click(function() {
                $('#rolesSelectionsGroup${position}').hide();
                $('#rolesSelectionsGroupShow${position}').show();
                return false;
            });
        });
    </script>
    <input type="radio" id="other${citation.externalId}" name="contributor${citation.externalId}" value="other" class="radioWithLabel" /><label for="other${citation.externalId}" class="labelForRadio"> Other Contribution</label><br />
    <div class="contribution_title">Please indicate the contribution that you made to this work</div>
    <div id="rolesSelectionsGroupShow${position}">
        <input id="rolesSelectionsGroupShowButton${position}" type="submit" class="submit" value="Show roles" style="display: none;" />
    </div>
    <div id="rolesSelectionsGroup${position}">
        <input id="rolesSelectionsGroupHideButton${position}" type="submit" class="submit" value="Hide roles" style="display: none;" />
        <div class="role_group_title"">Author</div><!-- CRO_0000001 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000019" roleLabel="Writing Original Draft" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000020" roleLabel="Editing and Proofreading" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000021" roleLabel="Figure Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000022" roleLabel="Translator" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000002" roleLabel="Background and Literature Search" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000003" roleLabel="Conceptualization" />
        </ul></div>
        <div class="role_group_title">Preservation</div><!-- CRO_0000004 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000023" roleLabel="Archivist" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000024" roleLabel="Digital Preservation" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000025" roleLabel="Conservator" />
        </ul></div>
        <div class="role_group_title">Data</div><!-- CRO_0000005 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000026" roleLabel="Data Curation" />
                <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000056" roleLabel="Metadata Application" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000027" roleLabel="Data Entry" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000028" roleLabel="Data Visualization" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000029" roleLabel="Data Analysis" />
                <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000057" roleLabel="Statistical Data Analysis" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000030" roleLabel="Data Collection" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000031" roleLabel="Data Aggregation" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000032" roleLabel="Data Integration" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000033" roleLabel="Data Quality Assurance" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000034" roleLabel="Data Modeling" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000035" roleLabel="Data Standards Developer" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000006" roleLabel="Funding Acquisition" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000007" roleLabel="Study Investigation" />
        </ul></div>
        <div class="role_group_title">Methodology</div><!-- CRO_0000008 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000036" roleLabel="Technique Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000037" roleLabel="Protocol Creation" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000038" roleLabel="Guideline Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000039" roleLabel="Standard Operating Procedure Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000040" roleLabel="Study Design" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000009" roleLabel="Project Management" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000010" roleLabel="Team Management" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000011" roleLabel="Regulatory Administration" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000012" roleLabel="Policy Development" />
        </ul></div>
        <div class="role_group_title">Communication</div><!-- CRO_0000014 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000041" roleLabel="Marketing" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000042" roleLabel="Networking Facilitation" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000043" roleLabel="Graphic Design" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000044" roleLabel="Website Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000045" roleLabel="Documentation" />
        </ul></div>
        <div class="role_group_title">Software Developer</div><!-- CRO_0000015 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000046" roleLabel="Software Architecture" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000047" roleLabel="Software Design" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000048" roleLabel="Computer Programming" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000049" roleLabel="Software Engineering" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000050" roleLabel="Software Testing" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000051" roleLabel="Software Project Management" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000052" roleLabel="Code Review" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000053" roleLabel="Technical Writing" />
        </ul></div>
        <div class="role_group_title">Information Technology Systems</div><!-- CRO_0000016 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000054" roleLabel="Software Systems" />
                <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000059" roleLabel="Database Administrator" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000055" roleLabel="Hardware Systems" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000017" roleLabel="Supervision" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000018" roleLabel="Validation" />
        </ul></div>
        <div class="role_group_title">Research Instrumentation</div><!-- CRO_0000060 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000066" roleLabel="Device Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000067" roleLabel="Equipment Technician" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000068" roleLabel="Survey and Questionnaire" />
        </ul></div>
        <div class="role_group_title">Educational</div><!-- CRO_0000061 -->
        <div class="role_group"><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000063" roleLabel="Educational Program Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000064" roleLabel="Educational Material Development" />
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000065" roleLabel="Teaching" />
        </ul></div>
        <div><ul class="roles">
            <@contributorRole citationId="${citation.externalId}" roleId="CRO_0000062" roleLabel="Intellectual Property Advisor" />
        </ul></div>
        <div class="role_group_title">If no suitable role(s) are listed, enter your own (comma or semi-colon separated</div>
        <div>
            <input type="textbox" style="width: 99%" id="otherRoles${citation.externalId}" name="otherRoles${citation.externalId}" />
        </div>
    </div>
    <br />
</#macro>