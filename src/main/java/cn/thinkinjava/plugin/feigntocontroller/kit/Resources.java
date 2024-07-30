package cn.thinkinjava.plugin.feigntocontroller.kit;


import com.intellij.ui.IconManager;

import javax.swing.*;

public class Resources {
//    public static final Icon TO_CONTROLLER_METHOD;
    public static final Icon JUMP_ARROW;
//    public static final Icon TO_XML;
//    public static final Icon TO_JAVA;

    private Resources() {
    }

    static {
//        TO_CONTROLLER_METHOD = Gutter.ImplementingMethod;

        // java.lang.NoSuchMethodError: 'javax.swing.Icon com.intellij.ui.IconManager.getIcon
        // ClassLoader 低版本的报错
        JUMP_ARROW = IconManager.getInstance().getIcon("/images/statement.png", Resources.class);

//        TO_XML = IconManager.getInstance().getIcon("/images/statement.png", Resources.class.getClassLoader());
//        TO_JAVA = IconManager.getInstance().getIcon("/images/mapper_method.png", Resources.class.getClassLoader());
    }
}
