package org.springframework.roo.addon.web.mvc.jsp.menu;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.web.mvc.jsp.roundtrip.XmlRoundTripFileManager;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generates the jsp menu and allows for management of menu items.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class MenuOperationsImpl implements MenuOperations {

    // Fields
    @Reference private FileManager fileManager;
    @Reference private ProjectOperations projectOperations;
    @Reference private PropFileOperations propFileOperations;
    @Reference private XmlRoundTripFileManager xmlRoundTripFileManager;

    public void addMenuItem(final JavaSymbolName menuCategoryName,
            final JavaSymbolName menuItemId, final String globalMessageCode,
            final String link, final String idPrefix,
            final LogicalPath logicalPath) {
        addMenuItem(menuCategoryName, menuItemId, "", globalMessageCode, link,
                idPrefix, false, logicalPath);
    }

    public void addMenuItem(final JavaSymbolName menuCategoryName,
            final JavaSymbolName menuItemId, final String menuItemLabel,
            final String globalMessageCode, final String link,
            final String idPrefix, final LogicalPath logicalPath) {
        addMenuItem(menuCategoryName, menuItemId, menuItemLabel,
                globalMessageCode, link, idPrefix, true, logicalPath);
    }

    private void addMenuItem(final JavaSymbolName menuCategoryName,
            final JavaSymbolName menuItemId, final String menuItemLabel,
            final String globalMessageCode, final String link, String idPrefix,
            final boolean writeProps, final LogicalPath logicalPath) {
        Assert.notNull(menuCategoryName, "Menu category name required");
        Assert.notNull(menuItemId, "Menu item name required");
        Assert.hasText(link, "Link required");

        Map<String, String> properties = new LinkedHashMap<String, String>();

        if (StringUtils.isBlank(idPrefix)) {
            idPrefix = DEFAULT_MENU_ITEM_PREFIX;
        }

        Document document = getMenuDocument(logicalPath);

        // Make the root element of the menu the one with the menu identifier
        // allowing for different decorations of menu
        Element rootElement = XmlUtils.findFirstElement("//*[@id='_menu']",
                document.getFirstChild());
        if (rootElement == null) {
            Element rootMenu = new XmlElementBuilder("menu:menu", document)
                    .addAttribute("id", "_menu").build();
            rootMenu.setAttribute("z",
                    XmlRoundTripUtils.calculateUniqueKeyFor(rootMenu));
            rootElement = (Element) document.getDocumentElement().appendChild(
                    rootMenu);
        }

        // Check for existence of menu category by looking for the identifier
        // provided
        final String lcMenuCategoryName = menuCategoryName.getSymbolName()
                .toLowerCase();

        Element category = XmlUtils.findFirstElement("//*[@id='c_"
                + lcMenuCategoryName + "']", rootElement);
        // If not exists, create new one
        if (category == null) {
            category = (Element) rootElement.appendChild(new XmlElementBuilder(
                    "menu:category", document).addAttribute("id",
                    "c_" + lcMenuCategoryName).build());
            category.setAttribute("z",
                    XmlRoundTripUtils.calculateUniqueKeyFor(category));
            properties.put("menu_category_" + lcMenuCategoryName + "_label",
                    menuCategoryName.getReadableSymbolName());
        }

        // Check for existence of menu item by looking for the identifier
        // provided
        Element menuItem = XmlUtils.findFirstElement("//*[@id='" + idPrefix
                + lcMenuCategoryName + "_"
                + menuItemId.getSymbolName().toLowerCase() + "']", rootElement);
        if (menuItem == null) {
            menuItem = new XmlElementBuilder("menu:item", document)
                    .addAttribute(
                            "id",
                            idPrefix + lcMenuCategoryName + "_"
                                    + menuItemId.getSymbolName().toLowerCase())
                    .addAttribute("messageCode", globalMessageCode)
                    .addAttribute("url", link).build();
            menuItem.setAttribute("z",
                    XmlRoundTripUtils.calculateUniqueKeyFor(menuItem));
            category.appendChild(menuItem);
        }
        if (writeProps) {
            properties.put("menu_item_" + lcMenuCategoryName + "_"
                    + menuItemId.getSymbolName().toLowerCase() + "_label",
                    menuItemLabel);
            propFileOperations.addProperties(projectOperations
                    .getPathResolver().getFocusedPath(Path.SRC_MAIN_WEBAPP),
                    "WEB-INF/i18n/application.properties", properties, true,
                    false);
        }
        xmlRoundTripFileManager.writeToDiskIfNecessary(
                getMenuFileName(logicalPath), document);
    }

    public void cleanUpFinderMenuItems(final JavaSymbolName menuCategoryName,
            final List<String> allowedFinderMenuIds,
            final LogicalPath logicalPath) {
        Assert.notNull(menuCategoryName, "Menu category identifier required");
        Assert.notNull(allowedFinderMenuIds,
                "List of allowed menu items required");

        Document document = getMenuDocument(logicalPath);

        // Find any menu items under this category which have an id that starts
        // with the menuItemIdPrefix
        List<Element> elements = XmlUtils.findElements("//category[@id='c_"
                + menuCategoryName.getSymbolName().toLowerCase()
                + "']//item[starts-with(@id, '" + FINDER_MENU_ITEM_PREFIX
                + "')]", document.getDocumentElement());
        if (elements.isEmpty()) {
            return;
        }
        for (Element element : elements) {
            if (!allowedFinderMenuIds.contains(element.getAttribute("id"))
                    && ("?".equals(element.getAttribute("z")) || XmlRoundTripUtils
                            .calculateUniqueKeyFor(element).equals(
                                    element.getAttribute("z")))) {
                element.getParentNode().removeChild(element);
            }
        }
        xmlRoundTripFileManager.writeToDiskIfNecessary(
                getMenuFileName(logicalPath), document);
    }

    /**
     * Attempts to locate a menu item and remove it.
     * 
     * @param menuCategoryName the identifier for the menu category (required)
     * @param menuItemName the menu item identifier (required)
     * @param idPrefix the prefix to be used for this menu item (optional,
     *            MenuOperations.DEFAULT_MENU_ITEM_PREFIX is default)
     */
    public void cleanUpMenuItem(final JavaSymbolName menuCategoryName,
            final JavaSymbolName menuItemName, String idPrefix,
            final LogicalPath logicalPath) {
        Assert.notNull(menuCategoryName, "Menu category identifier required");
        Assert.notNull(menuItemName, "Menu item id required");

        if (StringUtils.isBlank(idPrefix)) {
            idPrefix = DEFAULT_MENU_ITEM_PREFIX;
        }

        Document document = getMenuDocument(logicalPath);

        // Find menu item under this category if exists
        Element element = XmlUtils.findFirstElement("//category[@id='c_"
                + menuCategoryName.getSymbolName().toLowerCase()
                + "']//item[@id='" + idPrefix
                + menuCategoryName.getSymbolName().toLowerCase() + "_"
                + menuItemName.getSymbolName().toLowerCase() + "']",
                document.getDocumentElement());
        if (element == null) {
            return;
        }
        if ("?".equals(element.getAttribute("z"))
                || XmlRoundTripUtils.calculateUniqueKeyFor(element).equals(
                        element.getAttribute("z"))) {
            element.getParentNode().removeChild(element);
        }

        xmlRoundTripFileManager.writeToDiskIfNecessary(
                getMenuFileName(logicalPath), document);
    }

    private Document getMenuDocument(final LogicalPath logicalPath) {
        try {
            return XmlUtils.readXml(getMenuFileInputStream(logicalPath));
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse menu.jspx"
                    + (StringUtils.isBlank(e.getMessage()) ? "" : " ("
                            + e.getMessage() + ")"), e);
        }
    }

    private InputStream getMenuFileInputStream(final LogicalPath logicalPath) {
        String menuFileName = getMenuFileName(logicalPath);
        if (!fileManager.exists(menuFileName)) {
            try {
                FileCopyUtils.copy(
                        FileUtils.getInputStream(getClass(), "menu.jspx"),
                        fileManager.createFile(menuFileName).getOutputStream());
            }
            catch (Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of menu.jspx for MVC Menu addon.",
                        e);
            }
        }

        PathResolver pathResolver = projectOperations.getPathResolver();

        final String menuPath = pathResolver.getIdentifier(logicalPath,
                "WEB-INF/tags/menu/menu.tagx");
        if (!fileManager.exists(menuPath)) {
            try {
                fileManager.createOrUpdateTextFileIfRequired(menuPath,
                        FileCopyUtils.copyToString(new InputStreamReader(
                                FileUtils.getInputStream(getClass(),
                                        "menu.tagx"))), false);
            }
            catch (Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of menu.tagx for MVC Menu addon.",
                        e);
            }
        }

        final String itemPath = pathResolver.getIdentifier(logicalPath,
                "WEB-INF/tags/menu/item.tagx");
        if (!fileManager.exists(itemPath)) {
            try {
                fileManager.createOrUpdateTextFileIfRequired(itemPath,
                        FileCopyUtils.copyToString(new InputStreamReader(
                                FileUtils.getInputStream(getClass(),
                                        "item.tagx"))), false);
            }
            catch (Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of item.tagx for MVC Menu addon.",
                        e);
            }
        }

        final String categoryPath = pathResolver.getIdentifier(logicalPath,
                "WEB-INF/tags/menu/category.tagx");
        if (!fileManager.exists(categoryPath)) {
            try {
                fileManager.createOrUpdateTextFileIfRequired(categoryPath,
                        FileCopyUtils.copyToString(new InputStreamReader(
                                FileUtils.getInputStream(getClass(),
                                        "category.tagx"))), false);
            }
            catch (Exception e) {
                throw new IllegalStateException(
                        "Encountered an error during copying of category.tagx for MVC Menu addon.",
                        e);
            }
        }

        return fileManager.getInputStream(menuFileName);
    }

    private String getMenuFileName(final LogicalPath logicalPath) {
        return projectOperations.getPathResolver().getIdentifier(logicalPath,
                "WEB-INF/views/menu.jspx");
    }
}