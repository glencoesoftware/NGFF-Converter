package com.glencoesoftware.convert;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import picocli.CommandLine;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class WidgetFactory {

    private HashMap<Type, Method> handlers = new HashMap();

    public ArrayList<Node> generateWidgets(ArrayList<Method> controls){
//        ArrayList<Method> controls = test.getConfigurableMethods();
        ArrayList<Node> widgets = new ArrayList<Node>();
        for (Method m : controls) {
            CommandLine.Option cliOption = m.getAnnotation(CommandLine.Option.class);
            String name = m.getName().substring(3);
            name = name.replaceAll("\\d+", "").replaceAll("(.)([A-Z])", "$1 $2");
            widgets.add(new Label(name));
            String defaultValue = cliOption.defaultValue();
            if (defaultValue.equals("__no_default_value__")) {
                defaultValue = cliOption.fallbackValue();
            }
            Type settingType = m.getGenericParameterTypes()[0];

            if (settingType == int.class) {
                Integer value = 1;
                // Construct text area with int validation
            }


            widgets.add(getWidget(m, defaultValue));
            TextField tf = new TextField(defaultValue);
            tf.setTooltip(new Tooltip(Arrays.toString(cliOption.description())));
            widgets.add(tf);
        }
        return widgets;
    }


    public void getWidget(Method m) {
        CommandLine.Option cliOption = m.getAnnotation(CommandLine.Option.class);
        String name = m.getName().substring(3);
        name = name.replaceAll("\\d+", "").replaceAll("(.)([A-Z])", "$1 $2");
        String defaultValue = cliOption.defaultValue();


        Type settingType = m.getGenericParameterTypes()[0];
        Method generator = handlers.get(settingType);
        System.out.println(generator);

    }

    public Node getWidget(Method m, String currentValue) {
        CommandLine.Option cliOption = m.getAnnotation(CommandLine.Option.class);
        String name = m.getName().substring(3);
        name = name.replaceAll("\\d+", "").replaceAll("(.)([A-Z])", "$1 $2");
        String defaultValue = cliOption.defaultValue();
        if (defaultValue.equals("__no_default_value__")) {
            defaultValue = cliOption.fallbackValue();
        }
        TextField tf = new TextField(defaultValue);
        tf.setTooltip(new Tooltip(Arrays.toString(cliOption.description())));
        return tf;
    }

}
