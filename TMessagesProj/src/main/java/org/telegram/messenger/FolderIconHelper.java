package org.telegram.messenger;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FolderIconHelper {

    public static class IconOption {
        public final String emoticon;
        public final int resId;
        public final String label;

        public IconOption(String emoticon, int resId, String label) {
            this.emoticon = emoticon;
            this.resId = resId;
            this.label = label;
        }
    }

    public static final List<IconOption> FOLDER_ICONS = new ArrayList<>();
    private static final Map<String, Integer> EMOTICON_TO_RES = new HashMap<>();
    private static final Map<String, String> EMOTICON_TO_LABEL = new HashMap<>();

    static {
        addOption("📁", R.drawable.msg_folders, "Carpeta");
        addOption("💬", R.drawable.msg_discuss, "Chats");
        addOption("👤", R.drawable.msg_folders_private, "Privados");
        addOption("👥", R.drawable.msg_folders_groups, "Grupos");
        addOption("📢", R.drawable.msg_folders_channels, "Canales");
        addOption("🤖", R.drawable.msg_folders_bots, "Bots");
        addOption("✅", R.drawable.msg_folders_read, "No leídos");
        addOption("🔕", R.drawable.msg_folders_muted, "Silenciados");
        addOption("📦", R.drawable.msg_folders_archive, "Archivados");
        addOption("⭐", R.drawable.msg_fave, "Favoritos");
        addOption("🏠", R.drawable.msg_home, "Hogar");
        addOption("💼", R.drawable.msg_work, "Trabajo");
        addOption("📌", R.drawable.msg_pin, "Fijados");
        addOption("🔒", R.drawable.msg_secret, "Privado");
        addOption("🎭", R.drawable.msg_mask, "Personal");
        addOption("🙋", R.drawable.msg_folders_requests, "Solicitudes");

        // Common Telegram / Desktop folder emoticon aliases
        addAlias("🗂️", R.drawable.msg_folders);
        addAlias("🗂", R.drawable.msg_folders);
        addAlias("📂", R.drawable.msg_folders);
        addAlias("🗨️", R.drawable.msg_discuss);
        addAlias("🗨", R.drawable.msg_discuss);
        addAlias("💭", R.drawable.msg_discuss);
        addAlias("👨", R.drawable.msg_folders_private);
        addAlias("👩", R.drawable.msg_folders_private);
        addAlias("🧑", R.drawable.msg_folders_private);
        addAlias("👪", R.drawable.msg_folders_groups);
        addAlias("👨‍👩‍👧‍👦", R.drawable.msg_folders_groups);
        addAlias("📣", R.drawable.msg_folders_channels);
        addAlias("🔊", R.drawable.msg_folders_channels);
        addAlias("👾", R.drawable.msg_folders_bots);
        addAlias("✔", R.drawable.msg_folders_read);
        addAlias("✔️", R.drawable.msg_folders_read);
        addAlias("☑", R.drawable.msg_folders_read);
        addAlias("☑️", R.drawable.msg_folders_read);
        addAlias("🔇", R.drawable.msg_folders_muted);
        addAlias("❤", R.drawable.msg_fave);
        addAlias("❤️", R.drawable.msg_fave);
        addAlias("🌟", R.drawable.msg_fave);
        addAlias("🔥", R.drawable.msg_fave);
        addAlias("🏡", R.drawable.msg_home);
        addAlias("🏢", R.drawable.msg_work);
        addAlias("📍", R.drawable.msg_pin);
        addAlias("🔐", R.drawable.msg_secret);
        addAlias("🔑", R.drawable.msg_secret);
        addAlias("😎", R.drawable.msg_mask);
        addAlias("🎮", R.drawable.msg_mask);
        addAlias("🤝", R.drawable.msg_folders_requests);
    }

    private static void addOption(String emoticon, int resId, String label) {
        FOLDER_ICONS.add(new IconOption(emoticon, resId, label));
        EMOTICON_TO_RES.put(emoticon, resId);
        EMOTICON_TO_RES.put(stripVariation(emoticon), resId);
        EMOTICON_TO_LABEL.put(emoticon, label);
        EMOTICON_TO_LABEL.put(stripVariation(emoticon), label);
    }

    private static void addAlias(String emoticon, int resId) {
        EMOTICON_TO_RES.put(emoticon, resId);
        EMOTICON_TO_RES.put(stripVariation(emoticon), resId);
    }

    private static String stripVariation(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\uFE0F", "");
    }

    public static String getDefaultEmoticonForFlags(int flags) {
        boolean contacts = (flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0;
        boolean nonContacts = (flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0;
        boolean groups = (flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0;
        boolean channels = (flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0;
        boolean bots = (flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0;

        if ((contacts || nonContacts) && !groups && !channels && !bots) {
            return "👤";
        }
        if (groups && !contacts && !nonContacts && !channels && !bots) {
            return "👥";
        }
        if (channels && !contacts && !nonContacts && !groups && !bots) {
            return "📢";
        }
        if (bots && !contacts && !nonContacts && !groups && !channels) {
            return "🤖";
        }
        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0) {
            return "✅";
        }
        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0) {
            return "🔕";
        }
        if ((flags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0) {
            return "📦";
        }
        return "📁";
    }

    public static int getIconResId(String emoticon, int flags, boolean isDefault) {
        if (isDefault) {
            return R.drawable.msg_discuss;
        }
        if (!TextUtils.isEmpty(emoticon)) {
            Integer res = EMOTICON_TO_RES.get(emoticon);
            if (res == null) {
                res = EMOTICON_TO_RES.get(stripVariation(emoticon));
            }
            if (res != null) {
                return res;
            }
        }
        String fallback = getDefaultEmoticonForFlags(flags);
        Integer res = EMOTICON_TO_RES.get(fallback);
        return res != null ? res : R.drawable.msg_folders;
    }

    public static int getTabIconResId(int currentAccount, MessagesController.DialogFilter filter, boolean isDefault) {
        if (isDefault || filter == null || filter.isDefault()) {
            return R.drawable.msg_discuss;
        }
        String custom = KronoConfig.getFilterCustomIcon(currentAccount, filter.id);
        String emoticon = !TextUtils.isEmpty(custom) ? custom : filter.emoticon;
        return getIconResId(emoticon, filter.flags, false);
    }

    public static String getLabelForEmoticon(String emoticon) {
        if (TextUtils.isEmpty(emoticon)) {
            return "Automático";
        }
        String label = EMOTICON_TO_LABEL.get(emoticon);
        if (label == null) {
            label = EMOTICON_TO_LABEL.get(stripVariation(emoticon));
        }
        return label != null ? label : emoticon;
    }
}
