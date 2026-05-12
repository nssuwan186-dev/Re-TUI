package ohi.andre.consolelauncher.managers.notifications;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.RegexManager;
import ohi.andre.consolelauncher.managers.settings.NotificationSettings;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsList;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.resetFile;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationManager implements XMLPrefsElement {

    private static String COLOR_ATTRIBUTE = "color";
    public static String ENABLED_ATTRIBUTE = "enabled";
    public static String ID_ATTRIBUTE = "id";
    public static String FORMAT_ATTRIBUTE = "format";
    public static String FILTER_ATTRIBUTE = "filter";

    public static final String PATH = "notifications.xml";
    private static final String NAME = "NOTIFICATIONS";

    public boolean default_app_state;
    public String default_color;

    @Override
    public String[] delete() {
        return null;
    }

    @Override
    public XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
        values.add(save.label(), value);
    }

    @Override
    public String path() {
        return PATH;
    }

    private XMLPrefsList values;
    private List<NotificatedApp> apps;
    private List<FilterRule> filters;
    private List<XMLPrefsManager.IdValue> formats;

    public static NotificationManager instance = null;
    public static NotificationManager create(Context context) {
        if(instance == null) return new NotificationManager(context);
        else return instance;
    }

    private NotificationManager(Context context) {
        instance = this;

        apps = new ArrayList<>();
        filters = new ArrayList<>();
        formats = new ArrayList<>();
        values = new XMLPrefsList();

        try {
            File r = Tuils.getFolder();
            if(r == null) {
                Tuils.sendOutput(Color.RED, context, R.string.tuinotfound_notifications);
                return;
            }

            File file = new File(r, PATH);
            if(!file.exists()) {
                resetFile(file, NAME);
            }

            Object[] o;
            try {
                o = XMLPrefsManager.buildDocument(file, NAME);
                if(o == null) {
                    Tuils.sendXMLParseError(context, PATH);
                    return;
                }
            } catch (SAXParseException e) {
                Tuils.sendXMLParseError(context, PATH, e);
                return;
            } catch (Exception e) {
                Tuils.log(e);
                return;
            }

            Document d = (Document) o[0];
            Element root = (Element) o[1];

            List<Notifications> enums = new ArrayList<>(Arrays.asList(Notifications.values()));
            NodeList nodes = root.getElementsByTagName("*");

            String[] deleted = instance.delete();
            boolean needToWrite = false;

            for(int count = 0; count < nodes.getLength(); count++) {
                Node node = nodes.item(count);

                String nn = node.getNodeName();
                if (Tuils.find(nn, (List) enums) != -1) {
                    values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                    for (int en = 0; en < enums.size(); en++) {
                        if (enums.get(en).label().equals(nn)) {
                            enums.remove(en);
                            break;
                        }
                    }
                } else if (nn.equals(FILTER_ATTRIBUTE)) {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        String regex = XMLPrefsManager.getStringAttribute(e, VALUE_ATTRIBUTE);
                        if (regex == null) continue;
                        int filterId;
                        try {
                            filterId = e.hasAttribute(ID_ATTRIBUTE) ? Integer.parseInt(e.getAttribute(ID_ATTRIBUTE)) : -1;
                        } catch (NumberFormatException exc) {
                            filterId = -1;
                        }
                        Pattern pattern;
                        try {
                            int id = Integer.parseInt(regex);
                            pattern = RegexManager.instance.get(id).regex;
                        } catch (Exception exc) {
                            try {
                                pattern = Pattern.compile(regex);
                            } catch (Exception exc2) {
                                pattern = Pattern.compile(regex, Pattern.LITERAL);
                            }
                        }

                        filters.add(new FilterRule(filterId, regex, pattern));
                    }
                } else if(nn.equals(FORMAT_ATTRIBUTE)) {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        String format = XMLPrefsManager.getStringAttribute(e, VALUE_ATTRIBUTE);
                        if(format == null) continue;

                        int id;
                        try {
                            id = e.hasAttribute(ID_ATTRIBUTE) ? Integer.parseInt(e.getAttribute(ID_ATTRIBUTE)) : -1;
                        } catch (NumberFormatException f) {
                            continue;
                        }

                        formats.add(new XMLPrefsManager.IdValue(format, id));
                    }
                } else {
                    int index = deleted == null ? -1 : Tuils.find(nn, deleted);
                    if(index != -1) {
                        deleted[index] = null;
                        Element e = (Element) node;
                        root.removeChild(e);

                        needToWrite = true;
                    }

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;

                        NotificatedApp app;

                        boolean enabled = XMLPrefsManager.getBooleanAttribute(e, ENABLED_ATTRIBUTE);
                        String color = XMLPrefsManager.getStringAttribute(e, COLOR_ATTRIBUTE);
                        String format = XMLPrefsManager.getStringAttribute(e, FORMAT_ATTRIBUTE);

                        app = new NotificatedApp(nn, color, format, enabled);
                        apps.add(app);
                    }
                }
            }

            if (enums.size() > 0) {
                for (XMLPrefsSave s : enums) {
                    String value = s.defaultValue();

                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, value);
                    root.appendChild(em);

                    values.add(s.label(), value);
                }

                writeTo(d, file);
            } else if (needToWrite) {
                writeTo(d, file);
            }
        } catch (Exception e) {
            Tuils.log(e);
            Tuils.toFile(e);
        }

        for(NotificatedApp app : apps) {
            try {
                int formatID = Integer.parseInt(app.format);

                for(XMLPrefsManager.IdValue idValue : formats) {
                    if(idValue.id == formatID) {
                        app.format = idValue.value;
                        break;
                    }
                }
            } catch (Exception e) {}
        }

        default_app_state = NotificationSettings.appNotificationsEnabledByDefault();
        default_color = NotificationSettings.defaultColorRaw();
    }

    public void dispose() {

        if(values != null) {
            values.list.clear();
            values = null;
        }

        if(apps != null) {
            apps.clear();
            apps = null;
        }

        if(filters != null) {
            filters.clear();
            filters = null;
        }

        if(formats != null) {
            formats.clear();
            formats = null;
        }

        instance = null;
    }

    public static String setState(String pkg, boolean state) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {ENABLED_ATTRIBUTE}, new String[] {String.valueOf(state)});
    }

    public static String setColor(String pkg, String color) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {ENABLED_ATTRIBUTE, COLOR_ATTRIBUTE}, new String[] {String.valueOf(true), color});
    }

    public static String setFormat(String pkg, String format) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {FORMAT_ATTRIBUTE}, new String[] {format});
    }

    public static String addFilter(String pattern, int id) {
        File file = new File(Tuils.getFolder(), PATH);
        int filterId = id >= 0 ? id : nextFilterId(file);
        return XMLPrefsManager.add(file, FILTER_ATTRIBUTE, new String[] {ID_ATTRIBUTE, VALUE_ATTRIBUTE}, new String[] {String.valueOf(filterId), pattern});
    }

    public static String addLiteralFilter(String value) {
        if(value == null || value.length() == 0) return Tuils.EMPTYSTRING;
        return addFilter(Pattern.quote(value), -1);
    }

    public static String addFormat(String format, int id) {
        return XMLPrefsManager.add(new File(Tuils.getFolder(), PATH), FORMAT_ATTRIBUTE, new String[] {ID_ATTRIBUTE, VALUE_ATTRIBUTE}, new String[] {String.valueOf(id), format});
    }

    public static String rmFilter(int id) {
        return XMLPrefsManager.removeNode(new File(Tuils.getFolder(), PATH), FILTER_ATTRIBUTE, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});
    }

    public static String rmFormat(int id) {
        return XMLPrefsManager.removeNode(new File(Tuils.getFolder(), PATH), FORMAT_ATTRIBUTE, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(id)});
    }

    public boolean match(String text) {
//        if(pkg.equals(BuildConfig.APPLICATION_ID)) return true;

        for(FilterRule f : filters) {

            Matcher m = f.pattern.matcher(text);
            if(m.matches() || m.find() || text.equals(f.rawPattern)) {
                return true;
            }
        }

        return false;
    }

    public int apps() {
        return apps.size();
    }

    public String describeRules() {
        StringBuilder builder = new StringBuilder();
        builder.append("Default app state: ")
                .append(default_app_state ? "included" : "excluded")
                .append(Tuils.NEWLINE);

        builder.append("Included apps:");
        boolean foundIncluded = false;
        for(NotificatedApp app : apps) {
            if(app.enabled) {
                builder.append(Tuils.NEWLINE).append("- ").append(app.pkg);
                foundIncluded = true;
            }
        }
        if(!foundIncluded) builder.append(" []");

        builder.append(Tuils.NEWLINE).append("Excluded apps:");
        boolean foundExcluded = false;
        for(NotificatedApp app : apps) {
            if(!app.enabled) {
                builder.append(Tuils.NEWLINE).append("- ").append(app.pkg);
                foundExcluded = true;
            }
        }
        if(!foundExcluded) builder.append(" []");

        builder.append(Tuils.NEWLINE).append("Filters:");
        if(filters.isEmpty()) {
            builder.append(" []");
        } else {
            for(FilterRule filter : filters) {
                builder.append(Tuils.NEWLINE)
                        .append("- ");
                if (filter.id >= 0) {
                    builder.append("[").append(filter.id).append("] ");
                }
                builder.append(displayPattern(filter.rawPattern));
            }
        }

        return builder.toString();
    }

    public NotificatedApp getAppState(String pkg) {
        int index = Tuils.find(pkg, apps);
        if(index == -1) return null;
        return apps.get(index);

    }

    public static class NotificatedApp {
        String pkg, color, format;
        boolean enabled;

        public NotificatedApp(String pkg, String color, String format, boolean enabled) {
            this.pkg = pkg;
            this.color = color;
            this.enabled = enabled;
            this.format = format;
        }

        @Override
        public boolean equals(Object obj) {
            return this.toString().equals(obj.toString());
        }

        @Override
        public String toString() {
            return pkg;
        }
    }

    private static int nextFilterId(File file) {
        int max = -1;
        try {
            Object[] o = XMLPrefsManager.buildDocument(file, null);
            if(o == null) return 0;
            Element root = (Element) o[1];
            NodeList nodes = root.getElementsByTagName(FILTER_ATTRIBUTE);
            for(int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if(!(node instanceof Element)) continue;
                Element element = (Element) node;
                try {
                    max = Math.max(max, Integer.parseInt(element.getAttribute(ID_ATTRIBUTE)));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
        return max + 1;
    }

    private static String displayPattern(String rawPattern) {
        if(rawPattern == null) return Tuils.EMPTYSTRING;
        if(rawPattern.startsWith("\\Q") && rawPattern.endsWith("\\E")) {
            return rawPattern.substring(2, rawPattern.length() - 2)
                    .replace("\\E\\\\E\\Q", "\\E");
        }
        return rawPattern;
    }

    private static class FilterRule {
        final int id;
        final String rawPattern;
        final Pattern pattern;

        FilterRule(int id, String rawPattern, Pattern pattern) {
            this.id = id;
            this.rawPattern = rawPattern;
            this.pattern = pattern;
        }
    }
}
