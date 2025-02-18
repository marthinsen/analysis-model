package edu.hm.hafner.analysis.parser.gendarme;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.IssueParser;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.analysis.ReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.util.XmlElementUtil;

import static edu.hm.hafner.analysis.util.IntegerParser.*;

/**
 * Parses Gendarme violations.
 *
 * @author mathias.kluba@gmail.com
 */
public class GendarmeParser extends IssueParser {
    private static final long serialVersionUID = 1677715364464119907L;

    private static final Pattern FILE_PATTERN = Pattern.compile("^(.*)\\(.(\\d+)\\).*$");

    @Override
    public Report parse(final ReaderFactory factory) throws ParsingException {
        Document document = factory.readDocument();

        NodeList mainNode = document.getElementsByTagName("gendarme-output");

        Element rootElement = (Element) mainNode.item(0);
        Element resultsElement = (Element) rootElement.getElementsByTagName("results").item(0);
        Element rulesElement = (Element) rootElement.getElementsByTagName("rules").item(0);

        Map<String, GendarmeRule> rules = parseRules(XmlElementUtil.getChildElementsByName(rulesElement, "rule"));
        return parseViolations(XmlElementUtil.getChildElementsByName(resultsElement, "rule"), rules);
    }

    private Report parseViolations(final List<Element> ruleElements, final Map<String, GendarmeRule> rules) {
        try (IssueBuilder issueBuilder = new IssueBuilder()) {

            Report warnings = new Report();
            for (Element ruleElement : ruleElements) {
                String ruleName = ruleElement.getAttribute("Name");
                String problem = ruleElement.getElementsByTagName("problem").item(0).getTextContent();
                List<Element> targetElements = XmlElementUtil.getChildElementsByName(ruleElement, "target");

                GendarmeRule rule = rules.get(ruleName);
                if (rule != null) {
                    for (Element targetElement : targetElements) {
                        Element defectElement = (Element) targetElement.getElementsByTagName("defect").item(0);
                        String source = defectElement.getAttribute("Source");

                        String fileName = extractFileNameMatch(rule, source, 1);
                        Severity priority = extractPriority(defectElement);
                        int line = parseInt(extractFileNameMatch(rule, source, 2));

                        issueBuilder.setFileName(fileName)
                                .setLineStart(line)
                                .setCategory(rule.getName())
                                .setMessage(problem)
                                .setSeverity(priority);
                        warnings.add(issueBuilder.buildAndClean());
                    }
                }
            }
            return warnings;
        }
    }

    private Severity extractPriority(final Element defectElement) {
        switch (defectElement.getAttribute("Severity")) {
            case "Low":
                return Severity.WARNING_LOW;
            case "High":
                return Severity.WARNING_HIGH;
            default:
                return Severity.WARNING_NORMAL;
        }
    }

    private String extractFileNameMatch(final GendarmeRule rule, final String source, final int group) {
        String fileName = StringUtils.EMPTY;
        if (rule.getType() == GendarmeRuleType.Method) {
            Matcher matcher = FILE_PATTERN.matcher(source);
            if (matcher.matches()) {
                fileName = matcher.group(group);
            }
        }
        return fileName;
    }

    private Map<String, GendarmeRule> parseRules(final List<Element> ruleElements) {
        Map<String, GendarmeRule> rules = new HashMap<>();

        for (Element ruleElement : ruleElements) {
            GendarmeRule rule = new GendarmeRule();
            rule.setName(ruleElement.getAttribute("Name"));
            rule.setTypeName(ruleElement.getTextContent());

            String typeString = ruleElement.getAttribute("Type");
            if ("Type".equals(typeString)) {
                rule.setType(GendarmeRuleType.Type);
            }
            else if ("Method".equals(typeString)) {
                rule.setType(GendarmeRuleType.Method);
            }
            else if ("Assembly".equals(typeString)) {
                rule.setType(GendarmeRuleType.Assembly);
            }
            try {
                rule.setUrl(new URL(ruleElement.getAttribute("Uri")));
            }
            catch (MalformedURLException ignored) {
                rule.setUrl(null);
            }

            // add the rule to the cache
            rules.put(rule.getName(), rule);
        }

        return rules;
    }
}
