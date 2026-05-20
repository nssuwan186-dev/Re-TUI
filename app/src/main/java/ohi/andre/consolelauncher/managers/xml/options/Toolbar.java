package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */

public enum Toolbar implements XMLPrefsSave {

    show_toolbar {
        @Override
        public String defaultValue() {
            return "true";
        }

        @Override
        public String info() {
            return "If false, the toolbar is hidden";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    hide_toolbar_no_input {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, the toolbar will be hidden when the input field is empty";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    shortcut_button_1_enabled {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, the first custom toolbar button is shown";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    shortcut_button_1_command {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Command, alias, or app name executed by the first custom toolbar button";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }
    },
    shortcut_button_1_icon {
        @Override
        public String defaultValue() {
            return "star";
        }

        @Override
        public String info() {
            return "Icon key for the first custom toolbar button";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }
    },
    shortcut_button_2_enabled {
        @Override
        public String defaultValue() {
            return "false";
        }

        @Override
        public String info() {
            return "If true, the second custom toolbar button is shown";
        }

        @Override
        public String type() {
            return XMLPrefsSave.BOOLEAN;
        }
    },
    shortcut_button_2_command {
        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String info() {
            return "Command, alias, or app name executed by the second custom toolbar button";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }
    },
    shortcut_button_2_icon {
        @Override
        public String defaultValue() {
            return "star";
        }

        @Override
        public String info() {
            return "Icon key for the second custom toolbar button";
        }

        @Override
        public String type() {
            return XMLPrefsSave.TEXT;
        }
    };

    @Override
    public XMLPrefsElement parent() {
        return XMLPrefsManager.XMLPrefsRoot.TOOLBAR;
    }

    @Override
    public String label() {
        return name();
    }

    @Override
    public String[] invalidValues() {
        return null;
    }

    @Override
    public String getLowercaseString() {
        return label();
    }

    @Override
    public String getString() {
        return label();
    }
}
