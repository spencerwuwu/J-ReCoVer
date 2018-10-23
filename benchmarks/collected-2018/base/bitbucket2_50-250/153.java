// https://searchcode.com/api/result/128769030/

package ph.hum.spring.xml;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ph.hum.spring.factory.annotation.ComponentFactory;
import ph.hum.spring.xml.type.filter.ComponentFactoryAwareDelegatingTypeFilter;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.w3c.dom.Node.ELEMENT_NODE;

/**
 * This class provides a base implementation of {@link AbstractBeanDefinitionParser} which expects scans the configured
 * {@link #basePackage} for {@link ph.hum.spring.factory.annotation.ComponentFactory @ComponentFactory} annotated
 * classes.
 */
public class ComponentFactoryAwareBeanDefinitionParser extends AbstractBeanDefinitionParser {

    /**
     * The XML element to look for exclusions within.
     */
    protected static final String EXCLUDE_FILTER_ELEMENT = "exclude-filter";

    /**
     * The XML attribute to look for include/exclude values within.
     */
    protected static final String FILTER_EXPRESSION_ATTRIBUTE = "expression";

    /**
     * The XML attribute to look for include/exclude types within.
     */
    protected static final String FILTER_TYPE_ATTRIBUTE = "type";

    /**
     * The XML eleent to look for inclusions within.
     */
    protected static final String INCLUDE_FILTER_ELEMENT = "include-filter";

    /**
     * The base package to scan for
     * {@link ph.hum.spring.factory.annotation.ComponentFactory @ComponentFactory} annotated classes within. Subclasses
     * should set this value as part of their construction/initialization process.
     */
    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    private final String basePackage;

    /**
     * Creates a new bean definition parser, which will scan from the supplied base package.
     *
     * @param basePackage the base package to scan from.
     */
    public ComponentFactoryAwareBeanDefinitionParser(final String basePackage) {
        super();
        this.basePackage = basePackage;
    }

    /**
     * This method registers beans through component scanning of the {@link #basePackage} package, supporting filters
     * based on inclusion and exclusion roles. Filtering of components is performed via the
     * {@link ComponentFactoryAwareDelegatingTypeFilter} to properly filter the generated classes rather than the
     * factory beans themselves.
     *
     * @param element {@inheritDoc}
     * @param parserContext {@inheritDoc}
     *
     * @return always returns {@code null} - beans are registered via
     *         {@link ClassPathBeanDefinitionScanner#scan(String...)}
     */
    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
        final ClassPathBeanDefinitionScanner scanner = createScanner(parserContext);

        boolean unfiltered = true;
        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != ELEMENT_NODE) {
                continue;
            }
            final String localName = child.getLocalName();
            if (INCLUDE_FILTER_ELEMENT.equals(localName)) {
                addInclusionFilter(scanner, (Element) child);
                unfiltered = false;
            }
            else if (EXCLUDE_FILTER_ELEMENT.equals(localName)) {
                addExclusionFilter(scanner, (Element) child);
                unfiltered = false;
            }
            else {
                throw new IllegalArgumentException("Unknown child element '" + localName + "'");
            }
        }

        if (unfiltered) {
            scanner.addIncludeFilter(new AnnotationTypeFilter(ComponentFactory.class, true, true));
        }

        scanner.scan(basePackage);

        return null;
    }

    /**
     * Adds an exclusion filter to the supplied scanner based on the supplied element.
     *
     * @param scanner the {@link ClassPathBeanDefinitionScanner} to add the exclusion filter to.
     * @param element the {@link Element} to base the exclusion filter configuration on.
     */
    protected void addExclusionFilter(final ClassPathBeanDefinitionScanner scanner, final Element element) {
        final ClassLoader classLoader = getScannerClassLoader(scanner);
        final TypeFilter filter = createTypeFilter(element, classLoader);
        scanner.addExcludeFilter(filter);
    }

    /**
     * Adds an inclusion filter to the supplied scanner based on the supplied element.
     *
     * @param scanner the {@link ClassPathBeanDefinitionScanner} to add the inclusion filter to.
     * @param element the {@link Element} to base the inclusion filter configuration on.
     */
    protected void addInclusionFilter(final ClassPathBeanDefinitionScanner scanner, final Element element) {
        final ClassLoader classLoader = getScannerClassLoader(scanner);
        final TypeFilter filter = createTypeFilter(element, classLoader);
        scanner.addIncludeFilter(filter);
    }

    /**
     * This method creates a {@link TypeFilter} for the specified attribute type, which is then wrapped by a
     * {@link ComponentFactoryAwareDelegatingTypeFilter}.
     *
     * @param element the DOM element being parsed to create the type filter.
     * @param classLoader the classloader to use when instantiating classes (either for annotation checks or custom
     * type filters)
     *
     * @return a {@link TypeFilter} based on the element's specification, which has in turn been wrapped by a
     *         {@link ComponentFactoryAwareDelegatingTypeFilter}.
     */
    @SuppressWarnings("unchecked")
    protected TypeFilter createTypeFilter(final Element element, final ClassLoader classLoader) {
        final String filterType = element.getAttribute(FILTER_TYPE_ATTRIBUTE);
        final String expression = element.getAttribute(FILTER_EXPRESSION_ATTRIBUTE);

        try {
            if ("assignable".equals(filterType)) {
                final Class<?> assignableClass = classLoader.loadClass(expression);
                return new ComponentFactoryAwareDelegatingTypeFilter(new AssignableTypeFilter(assignableClass));
            }

            if ("custom".equals(filterType)) {
                final Class<?> filterClass = classLoader.loadClass(expression);
                if (!TypeFilter.class.isAssignableFrom(filterClass)) {
                    final String message = format("Class is not assignable to [%s]: %s",
                                                  TypeFilter.class.getName(),
                                                  expression);
                    throw new IllegalArgumentException(message);
                }
                return new ComponentFactoryAwareDelegatingTypeFilter(BeanUtils.<TypeFilter>instantiate((Class<TypeFilter>) filterClass));
            }

            if ("regex".equals(filterType)) {
                return new ComponentFactoryAwareDelegatingTypeFilter(new RegexPatternTypeFilter(compile(expression)));
            }
        }
        catch (ClassNotFoundException e) {
            throw new FatalBeanException("Type filter class not found: " + expression, e);
        }

        throw new IllegalArgumentException("Unsupported filter type: " + filterType);
    }

    /**
     * This method returns the classloader from the supplied scanner. It also exists purely to reduce the footprint of
     * un-unit-testable code.
     *
     * @param scanner The scanner to return the classloader of.
     *
     * @return the classloader of the associated scanner.
     */
    @SuppressWarnings("PMD.UseProperClassLoader")
    protected ClassLoader getScannerClassLoader(final ClassPathBeanDefinitionScanner scanner) {
        final ResourceLoader resourceLoader = scanner.getResourceLoader();
        return resourceLoader.getClassLoader();
    }

    /**
     * This method creates the scanner to use in configuring bean definitions in the
     * {@link #parseInternal(Element, ParserContext)} method. It exists purely to minimise the footprint of untested
     * code the results from the scanner itself being relatively hard to mock and/or configure for a unit test.
     *
     * @param parserContext the context used to create the scanner.
     *
     * @return a {@link ClassPathBeanDefinitionScanner} with the bare bones configuration from the supplied parser
     *         context.
     */
    protected ClassPathBeanDefinitionScanner createScanner(final ParserContext parserContext) {
        final BeanDefinitionRegistry registry = parserContext.getRegistry();
        final BeanDefinitionParserDelegate parserDelegate = parserContext.getDelegate();
        final Environment environment = parserDelegate.getEnvironment();

        return new ClassPathBeanDefinitionScanner(registry, false, environment);
    }

}

