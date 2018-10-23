// https://searchcode.com/api/result/124965145/

package com.atlassian.plugins.codegen.modules.confluence.blueprint;

import com.atlassian.plugins.codegen.AbstractModuleCreatorTestCase;
import com.atlassian.plugins.codegen.ClassId;
import com.atlassian.plugins.codegen.ComponentDeclaration;
import com.atlassian.plugins.codegen.PluginProjectChangeset;
import com.atlassian.plugins.codegen.ResourceFile;
import com.atlassian.plugins.codegen.SourceFile;
import com.atlassian.plugins.codegen.modules.AbstractNameBasedModuleProperties;
import com.atlassian.plugins.codegen.modules.common.web.WebResourceProperties;
import com.google.common.collect.Lists;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.atlassian.plugins.codegen.modules.confluence.blueprint.BlueprintI18nProperty.*;
import static com.atlassian.plugins.codegen.modules.confluence.blueprint.BlueprintPromptEntry.*;
import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests that the {@link BlueprintModuleCreator}, given a valid set of {@link BlueprintProperties}, creates the correct
 * plugin modules and files.
 *
 * NOTE - this test uses the {@link BlueprintBuilder} to generate the {@link BlueprintProperties} passed to the
 * {@link BlueprintModuleCreator}, which makes this test somewhat of an Integration test. If tests in this class start
 * failing, check for unit-test failures in the BlueprintBuilderTest.
 *
 * @since 4.1.8
 */
public class BlueprintModuleCreatorTest extends AbstractModuleCreatorTestCase<BlueprintProperties>
{
    public static final String PLUGIN_KEY = "com.atlassian.confluence.plugins.foo-print";
    /**
     *  We use the builder to turn simple prompt properties into complex BlueprintProperties objects and reduce
     *  duplication in the test. This means that we assume the Builder does its job correctly - this is the
     *  responsibility of the BlueprintBuilder unit test.
     */
    private BlueprintPromptEntries promptProps;

    private BlueprintProperties blueprintProps;

    // The expected values for the Properties and created plugin content
    private String blueprintModuleKey = "foo-print-blueprint";
    private String blueprintIndexKey = "foo-print";
    private String webItemName = "FooPrint";
    private String webItemDesc = "There's no Blueprint like my FooPrint.";
    private String templateModuleKey = "foo-plate";

    public BlueprintModuleCreatorTest()
    {
        super("blueprint", new BlueprintModuleCreator());
    }

    @Before
    public void setupBaseProps() throws Exception
    {
        createBasePromptProperties();
        buildBlueprintProperties();

    }

    private void buildBlueprintProperties()
    {
        // The inherited tests assume that we have a BlueprintProperties in setup, so the object has
        // to exist before our tests can update the promptProps and build a new one. The workaround is to recreate
        // the properties in the tests that need them.
        changeset = null;
        blueprintProps = new BlueprintBuilder(promptProps).build();
        setProps(blueprintProps);
    }

    /**
     * Common properties for all tests - creates the most basic combination of blueprint, content-template and
     * web-item elements / resources that will result in a browser-testable Blueprint.
     */
    private void createBasePromptProperties()
    {
        promptProps = new BlueprintPromptEntries(PLUGIN_KEY);

        promptProps.put(INDEX_KEY_PROMPT, blueprintIndexKey);
        promptProps.put(WEB_ITEM_NAME_PROMPT, webItemName);
        promptProps.put(WEB_ITEM_DESC_PROMPT, webItemDesc);

        List<String> contentTemplateKeys = Lists.newArrayList();
        contentTemplateKeys.add(templateModuleKey);
        promptProps.put(CONTENT_TEMPLATE_KEYS_PROMPT, contentTemplateKeys);
    }

    @After
    public void tearDown()
    {
        changeset = null;
    }

    @Test
    public void blueprintModuleBasicSettings() throws Exception
    {
        Element blueprintModule = getGeneratedModule();
        assertNodeText(blueprintModule, "@key", blueprintModuleKey);
        String blueprintModuleName = "FooPrint Blueprint";
        assertNodeText(blueprintModule, "@name", blueprintModuleName);
        assertNodeText(blueprintModule, "@index-key", blueprintIndexKey);
        assertNodeText(blueprintModule, "content-template/@ref", templateModuleKey);

        String indexPageI18nTitle = PLUGIN_KEY + ".index.page.title";
        assertNodeText(blueprintModule, "@i18n-index-title-key", indexPageI18nTitle);

        assertI18nString(indexPageI18nTitle, "FooPrints");
    }

    @Test
    public void contentTemplateModuleBasicSettings() throws Exception
    {
        Element templateModule = getGeneratedModule("content-template");
        assertNodeText(templateModule, "@key", templateModuleKey);
        assertNodeText(templateModule, "@i18n-name-key", PLUGIN_KEY + "." + "foo-plate.name");
        assertNodeText(templateModule, "description/@key", PLUGIN_KEY + "." + "foo-plate.desc");

        assertNodeText(templateModule, "resource/@name", "template");
        assertNodeText(templateModule, "resource/@type", "download");
        assertNodeText(templateModule, "resource/@location", "xml/" + templateModuleKey + ".xml");

        assertI18nString(PLUGIN_KEY + "." + templateModuleKey + ".name", "FooPrint Content Template 0");
        String templateDesc = "Contains Storage-format XML used by the FooPrint Blueprint";
        assertI18nString(PLUGIN_KEY + "." + templateModuleKey + ".desc", templateDesc);
    }

    @Test
    public void contentTemplateFileIsCreated() throws Exception
    {
        ResourceFile file = getResourceFile("xml", templateModuleKey + ".xml");
        String xml = new String(file.getContent());
        String templateContentI18nKey = PLUGIN_KEY + "." + templateModuleKey + ".content.text";
        assertThat(xml, containsString(templateContentI18nKey));
        assertThat(xml, containsString("ac:placeholder"));
        assertThat(xml, containsString("template.placeholder"));
        assertThat(xml, containsString("ac:type=\"mention\""));
        assertThat(xml, containsString("template.mention.placeholder"));
        assertI18nString(templateContentI18nKey, ContentTemplateProperties.CONTENT_I18N_DEFAULT_VALUE);
    }

    @Test
    public void indexPageTemplateFileIsCreated() throws Exception
    {
        promptProps.put(INDEX_PAGE_TEMPLATE_PROMPT, true);
        buildBlueprintProperties();

        Element blueprintModule = getGeneratedModule();
        String templateModuleKey = BlueprintProperties.INDEX_TEMPLATE_DEFAULT_KEY;
        assertNodeText(blueprintModule, "@index-template-key", templateModuleKey);

        Document templateModules = getAllGeneratedModulesOfType("content-template");
        Element templateModule = (Element)templateModules.selectSingleNode("//content-template[@key='custom-index-page-template']");
        assertNodeText(templateModule, "@key", templateModuleKey);
        assertNodeText(templateModule, "@i18n-name-key", "com.atlassian.confluence.plugins.foo-print.custom-index-page-template.name");
        assertNodeText(templateModule, "description/@key", "com.atlassian.confluence.plugins.foo-print.custom-index-page-template.desc");

        assertNodeText(templateModule, "resource/@name", "template");
        assertNodeText(templateModule, "resource/@type", "download");
        assertNodeText(templateModule, "resource/@location", "xml/" + templateModuleKey + ".xml");

        assertI18nString("com.atlassian.confluence.plugins.foo-print.custom-index-page-template.name", "Custom Index Page Content Template");
        String templateDesc = "Contains Storage-format XML used by the FooPrint Blueprint's Index page";
        assertI18nString("com.atlassian.confluence.plugins.foo-print.custom-index-page-template.desc", templateDesc);

        ResourceFile file = getResourceFile("xml", templateModuleKey + ".xml");
        String xml = new String(file.getContent());
        String templateContentI18nKey = PLUGIN_KEY + "." + templateModuleKey + ".content.text";
        assertThat(xml, containsString(templateContentI18nKey));
        assertI18nString(templateContentI18nKey, ContentTemplateProperties.INDEX_TEMPLATE_CONTENT_VALUE);
    }

    @Test
    public void contextProviderIsAddedToContentTemplate() throws Exception
    {
        promptProps.put(CONTEXT_PROVIDER_PROMPT, true);
        buildBlueprintProperties();

        // 1. Context provider is added to content-template element
        Element templateModule = getGeneratedModule("content-template");
        String className = "ContentTemplateContextProvider";
        String packageName = "com.atlassian.confluence.plugins.foo_print";  // foo-print without the '-'
        String fqClassName = packageName + "." + className;
        assertNodeText(templateModule, "context-provider/@class", fqClassName);

        // 2. ContextProviderProperties Java class is added.
        SourceFile sourceFile = getSourceFile(packageName, className);
        assertThat(sourceFile.getContent(), containsString("package " + packageName + ";"));
        assertThat(sourceFile.getContent(), containsString("\"variableA\""));
        assertThat(sourceFile.getContent(), containsString("\"variableB\""));

        // 3. Context added by provider is referenced in the template.
        ResourceFile file = getResourceFile("xml", templateModuleKey + ".xml");
        String xml = new String(file.getContent());
        assertThat(xml, containsString("<at:var at:name=\"variableA\" />"));
        assertThat(xml, containsString("<at:var at:name=\"variableB\" at:rawxhtml=\"true\" />"));
    }

    @Test
    public void webItemModuleBasicSettings() throws Exception
    {
        String webItemNameI18nKey = PLUGIN_KEY + ".blueprint.display.name";
        String webItemDescI18nKey = PLUGIN_KEY + ".blueprint.display.desc";

        Element module = getGeneratedModule("web-item");
        assertNameBasedModuleProperties(module, blueprintProps.getWebItem());

        assertNodeText(module, "@key", "foo-print-blueprint-web-item");
        assertNodeText(module, "@i18n-name-key", webItemNameI18nKey);
        assertNodeText(module, "@section", "system.create.dialog/content");
        assertNodeText(module, "description/@key", webItemDescI18nKey);
        assertNodeText(module, "param/@name", BlueprintProperties.WEB_ITEM_BLUEPRINT_KEY);
        assertNodeText(module, "param/@value", blueprintModuleKey);

        assertI18nString(webItemNameI18nKey, webItemName);
        assertI18nString(webItemDescI18nKey, webItemDesc);
    }

    @Test
    public void webItemIconIsCreated() throws Exception
    {
        Element webItem = getGeneratedModule("web-item");
        assertNodeText(webItem, "styleClass", "icon-foo-print-blueprint large");

        Element webResource = getGeneratedModule("web-resource");
        assertNodeText(webResource, "resource[@name='blueprints.css']/@type", "download");
        assertNodeText(webResource, "resource[@name='blueprints.css']/@location", "css/blueprints.css");

        String css = new String(getResourceFile("css", "blueprints.css").getContent());
        assertThat(css, containsString(".icon-foo-print-blueprint.large {"));
        assertThat(css, containsString(".acs-nav-item.blueprint.foo-print .icon {"));
    }

    @Test
    public void howToUseTemplateIsAdded() throws Exception
    {
        promptProps.put(HOW_TO_USE_PROMPT, true);
        buildBlueprintProperties();

        // 1. The blueprint element should have a new attribute with the how-to-use template reference
        Element blueprintModule = getGeneratedModule();
        assertNodeText(blueprintModule, "@how-to-use-template", blueprintProps.getHowToUseTemplate());

        // 2. There should be a Soy file containing the referenced template
        String soyHeadingI18nKey = PLUGIN_KEY + ".wizard.how-to-use.heading";
        String soyContentI18nKey = PLUGIN_KEY + ".wizard.how-to-use.content";
        String soy = new String(getResourceFile("soy", "my-templates.soy").getContent());
        assertThat(soy, containsString(format("{namespace %s}", "Confluence.Blueprints.Plugin.FooPrint")));
        assertThat(soy, containsString("{template .howToUse}"));
        assertThat(soy, containsString(format("{getText('%s')}", soyHeadingI18nKey)));
        assertThat(soy, containsString(format("{getText('%s')}", soyContentI18nKey)));

        // 3. There should be a web-resource pointing to the new file
        Element webResourceModule = getGeneratedModule("web-resource");
        assertWebResource(webResourceModule, blueprintProps.getWebResource());

        // 4. There should new entries in the i18n file for the template
        assertI18nString(soyHeadingI18nKey, HOW_TO_USE_HEADING.getI18nValue());
        assertI18nString(soyContentI18nKey, HOW_TO_USE_CONTENT.getI18nValue());
    }

    @Test
    public void editorIsSkipped() throws Exception
    {
        promptProps.put(SKIP_PAGE_EDITOR_PROMPT, true);
        buildBlueprintProperties();

        Element blueprintModule = getGeneratedModule();
        assertNodeText(blueprintModule, "@create-result", BlueprintProperties.CREATE_RESULT_VIEW);

        // If the edior is skipped there should be no placeholders in the generated template XML file
        String xml = new String(getResourceFile("xml", templateModuleKey + ".xml").getContent());
        assertThat(xml, not(containsString("ac:placeholder")));
    }

    @Test
    public void dialogWizardIsAdded() throws Exception
    {
        promptProps.put(DIALOG_WIZARD_PROMPT, true);
        buildBlueprintProperties();

        // 1. The blueprint element should have a new dialog-wizard element and children
        Element blueprintModule = getGeneratedModule();
        Element wizardElement = blueprintModule.element("dialog-wizard");
        assertNotNull("dialog-wizard element should be created", wizardElement);
        assertNodeText(wizardElement, "@key", "foo-print-wizard");

        Element pageElement = wizardElement.element("dialog-page");
        assertNotNull("dialog-page element should be created", pageElement);
        assertNodeText(pageElement, "@id", "page0");
        assertNodeText(pageElement, "@template-key", "Confluence.Blueprints.Plugin.FooPrint.wizardPage0");

        String wizardPageTitle = PLUGIN_KEY + ".wizard.page0.title";
        String wizardPageDescHeader = PLUGIN_KEY + ".wizard.page0.desc.header";
        String wizardPageDescContent = PLUGIN_KEY + ".wizard.page0.desc.content";
        assertNodeText(pageElement, "@title-key", wizardPageTitle);
        assertNodeText(pageElement, "@description-header-key", wizardPageDescHeader);
        assertNodeText(pageElement, "@description-content-key", wizardPageDescContent);
        assertI18nString(wizardPageTitle, "Wizard Page 0 Title");
        assertI18nString(wizardPageDescHeader, "Page 0 Description");
        assertI18nString(wizardPageDescContent, "This wizard page does A, B and C");

        // 2. There should be a Soy file containing the referenced template
        String soy = new String(getResourceFile("soy", "my-templates.soy").getContent());
        String fieldId = "foo-print-blueprint-page-title";
        assertThat(soy, containsString(format("{namespace %s}", "Confluence.Blueprints.Plugin.FooPrint")));
        assertThat(soy, containsString("{template .wizardPage0}"));
        assertThat(soy, containsString(fieldId));

        // 3. There should be a JS file containing wizard callbacks
        String js = new String(getResourceFile("js", "dialog-wizard.js").getContent());
        assertThat(js, containsString("setWizard('" + PLUGIN_KEY + ":foo-print-blueprint-web-item"));
        assertThat(js, containsString(fieldId));
        
        // 4. There should be a web-resource pointing to the new files
        Element webResourceModule = getGeneratedModule("web-resource");
        assertWebResource(webResourceModule, blueprintProps.getWebResource());

        // 5. There should be new entries in the i18n file for the JS, and matching references in the Soy and JS files
        String titleLabel = PLUGIN_KEY + ".wizard.page0.title.label";
        String titlePlace = PLUGIN_KEY + ".wizard.page0.title.placeholder";
        String titleError = PLUGIN_KEY + ".wizard.page0.title.error";
        String preRender  = PLUGIN_KEY + ".wizard.page0.pre-render";
        String postRender = PLUGIN_KEY + ".wizard.page0.post-render";

        assertThat(soy, containsString(format("{getText('%s')}", titleLabel)));
        assertThat(soy, containsString(format("{getText('%s')}", titlePlace)));
        assertThat(js, containsString(format("AJS.I18n.getText('%s')", preRender)));
        assertThat(js, containsString(format("AJS.I18n.getText('%s')", postRender)));
        assertThat(js, containsString(format("AJS.I18n.getText('%s')", titleError)));

        // 6. There should be an at:var for the wizardVariable field in the wizard
        String xml = new String(getResourceFile("xml", templateModuleKey + ".xml").getContent());
        assertThat(xml, containsString("wizardVariable"));

        // The combination of Velocity, JavaScript and jQuery can lead to problems if variable names start with "$".
        // Using the correct escape characters in Velocity should fix this - test that the rendered JS is correct.
        assertThat(js, containsString("state.$container;"));
        assertThat(js, not(containsString("(.trim")));
        assertThat(js, not(containsString("\\$")));
        assertThat(js, not(containsString("${")));

        assertI18nString(titleLabel, WIZARD_FORM_TITLE_FIELD_LABEL.getI18nValue());
        assertI18nString(titlePlace, WIZARD_FORM_TITLE_FIELD_PLACEHOLDER.getI18nValue());
        assertI18nString(preRender, WIZARD_FORM_PRE_RENDER_TEXT.getI18nValue());
        assertI18nString(postRender, WIZARD_FORM_POST_RENDER_TEXT.getI18nValue());
        assertI18nString(titleError, WIZARD_FORM_TITLE_FIELD_ERROR.getI18nValue());
    }

    @Test
    public void eventListenerIsAdded() throws Exception
    {
        promptProps.put(EVENT_LISTENER_PROMPT, true);
        buildBlueprintProperties();

        // 1. Listener Component should be added to plugin XML
        String packageName = "com.atlassian.confluence.plugins.foo_print";
        String className = "BlueprintCreatedListener";
        ClassId classId = ClassId.packageAndClass(packageName, className);
        ComponentDeclaration component = getComponentOfClass(classId);
        assertThat(component.getKey(), is("blueprint-created-listener"));
        assertThat(component.getName().get(), is("Blueprint Created Event Listener"));

        // 2. Listener class should be added
        SourceFile sourceFile = getSourceFile(packageName, className);
        assertThat(sourceFile.getContent(), containsString("package " + packageName + ";"));
        assertThat(sourceFile.getContent(), containsString(PLUGIN_KEY));
    }

    private void assertWebResource(Element element, WebResourceProperties expectedResource)
    {
        assertNameBasedModuleProperties(element, expectedResource);

        assertNodeText(element, "context[1]", expectedResource.getContexts().get(0));
        assertNodeText(element, "context[2]", expectedResource.getContexts().get(1));

        assertNodeText(element, "dependency", expectedResource.getDependencies().get(0));

        assertNotNull(element.selectSingleNode("transformation"));
        assertNodeText(element, "transformation[@extension='soy']/transformer/@key", "soyTransformer");
        assertNodeText(element, "transformation[@extension='soy']/transformer/functions",
            "com.atlassian.confluence.plugins.soy:soy-core-functions");

        // Only check for JS if added to the expected web-resource
        if (expectedResource.getTransformations().size() == 2)
        {
            assertNodeText(element, "transformation[@extension='js']/transformer/@key", "jsI18n");
        }
    }

    private void assertNameBasedModuleProperties(Element element, AbstractNameBasedModuleProperties props)
    {
        assertNodeText(element, "@key", props.getModuleKey());
        assertNodeText(element, "@name", props.getModuleName());
        assertNodeText(element, "@i18n-name-key", props.getNameI18nKey());
        assertNodeText(element, "description/@key", props.getDescriptionI18nKey());
        assertNodeText(element, "description", props.getDescription());
    }

    // Cache the changeset during each test.
    @Override
    protected PluginProjectChangeset getChangesetForModule() throws Exception
    {
        if (changeset == null)
        {
            changeset = super.getChangesetForModule();
        }
        return changeset;
    }

    private void assertNodeText(Element element, String nodePath, String expectedText)
    {
        assertEquals(expectedText, getText(element, nodePath));
    }

    private String getText(Element element, String nodePath)
    {
        Node node = element.selectSingleNode(nodePath);
        assertNotNull("Couldn't find node with path: " + nodePath, node);
        return node.getText();
    }

    // I prefer this method name - getI18nString doesn't indicate that an Assert is being done. dT
    private void assertI18nString(String i18nKey, String value) throws Exception
    {
        getI18nString(i18nKey, value);
    }
}

