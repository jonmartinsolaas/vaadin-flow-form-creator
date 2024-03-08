package com.martinsolaas.formcreator;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.shared.InputField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@SuppressWarnings("ALL")
@Slf4j
public class FormCreator {

    public static <T extends Component & HasComponents, U> void bindAndCreateFields(T layout, BeanValidationBinder<U> binder, U bean) {
        Arrays.stream(bean.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(FieldOrder.class))
                .sorted(Comparator.comparing(field -> field.getAnnotation(FieldOrder.class).value()))
                .forEach(field -> createFieldAndBindField(field, binder, layout));
    }

    @SneakyThrows
    private static <T extends Component & HasComponents> void createFieldAndBindField(Field modelField, Binder<?> binder, T layout) {

        Class<?> modelFieldClass = modelField.getType();
        Optional<Class<? extends InputField<?, ?>>> specifiedInputFieldType;

        if (modelField.getAnnotation(FieldType.class) != null)
            specifiedInputFieldType = Optional.of(modelField.getAnnotation(FieldType.class).value());
        else
            specifiedInputFieldType = Optional.empty();

        InputField inputField = null;
        String label = modelField.getAnnotation(FieldLabel.class) != null
                ? modelField.getAnnotation(FieldLabel.class).value() : modelField.getName();

        if (specifiedInputFieldType.isEmpty()) { // we will try to deduce appropriate input field type
            if (modelFieldClass == String.class) {
                inputField = new TextField(label);
            } else if (modelFieldClass == Double.class || modelFieldClass == Integer.class
                    || modelFieldClass == Short.class || modelFieldClass == Float.class) {
                inputField = new NumberField(label);
            } else if (modelFieldClass == Boolean.class) {
                inputField = new Checkbox(label);
            } else if (modelFieldClass == LocalDate.class) {
                inputField = new DatePicker(label);
            } else if (modelFieldClass == LocalDateTime.class) {
                inputField = new DateTimePicker(label);
            } else if (modelFieldClass == LocalTime.class) {
                inputField = new TimePicker(label);
            } else if (modelFieldClass.isEnum()) {
                ComboBox<Enum<?>> combobox = new ComboBox<>(label);
                combobox.setItems((Enum<?>[])modelFieldClass.getEnumConstants());
                inputField = combobox;
            } else {
                throw new RuntimeException("Unsupported member type " + modelFieldClass.getName());
            }
        } else { // user has specified input field type
            Class<? extends InputField<?, ?>> specifiedComponentType = specifiedInputFieldType.get();
            inputField = specifiedComponentType.getDeclaredConstructor().newInstance();
            inputField.setLabel(modelField.getAnnotation(FieldLabel.class) != null ? modelField.getAnnotation(FieldLabel.class).value() : modelField.getName());
        }

        if (modelField.getAnnotation(FieldWidth.class) != null && inputField != null && inputField instanceof HasSize) {
            ((HasSize) inputField).setWidth(modelField.getAnnotation(FieldWidth.class).value());
        }

        Binder.BindingBuilder bindingBuilder = binder.forField((HasValue) inputField);

        if (modelFieldClass == Integer.class) {
            bindingBuilder.withConverter(new DoubleToIntegerConverter());
        } else if (modelFieldClass == Short.class) {
            bindingBuilder.withConverter(new DoubleToShortConverter());
        } else if (modelFieldClass == Float.class) {
            bindingBuilder.withConverter(new DoubleToFloatConverter());
        } else if (isParameterizedListOfType(modelField.getGenericType(), Integer.class)) {
            bindingBuilder.withConverter(new StringToIntegerListConverter());
        } else if (isParameterizedListOfType(modelField.getGenericType(), String.class)) {
            bindingBuilder.withConverter(new StringToStringListConverter());
        }

        bindingBuilder.bind(modelField.getName());
        layout.add((Component) inputField);
    }

    private static class DoubleToIntegerConverter implements Converter<Double, Integer> {

        @Override
        public Result<Integer> convertToModel(Double value, ValueContext context) {
            if (value == null) return Result.ok(null);
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
                return Result.error("Integer value out of range");
            return Result.ok(value.intValue());
        }

        @Override
        public Double convertToPresentation(Integer value, ValueContext context) {
            if (value == null) return null;
            return value.doubleValue();
        }
    }

    private static class DoubleToShortConverter implements Converter<Double, Short> {

        @Override
        public Result<Short> convertToModel(Double value, ValueContext context) {
            if (value == null) return Result.ok(null);
            if (value > Short.MAX_VALUE || value < Short.MIN_VALUE)
                return Result.error("Short value out of range");
            return Result.ok(value.shortValue());
        }

        @Override
        public Double convertToPresentation(Short value, ValueContext context) {
            if (value == null) return null;
            return value.doubleValue();
        }
    }

    private static class DoubleToFloatConverter implements Converter<Double, Float> {

        @Override
        public Result<Float> convertToModel(Double value, ValueContext context) {
            if (value == null) return Result.ok(null);
            if (value > Float.MAX_VALUE || value < Float.MIN_VALUE)
                return Result.error("Float value out of range");
            return Result.ok(value.floatValue());
        }

        @Override
        public Double convertToPresentation(Float value, ValueContext context) {
            if (value == null) return null;
            return value.doubleValue();
        }
    }

    private static class StringToIntegerListConverter implements Converter<String, List<Integer>> {

        @Override
        public Result<List<Integer>> convertToModel(String value, ValueContext context) {
            try {
                List<Integer> integers = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                return Result.ok(integers);
            } catch (NumberFormatException e) {
                return Result.error("Invalid format for integer list");
            }
        }

        @Override
        public String convertToPresentation(List<Integer> value, ValueContext context) {
            return value.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
    }

    public static class StringToStringListConverter implements Converter<String, List<String>> {

        @Override
        public Result<List<String>> convertToModel(String value, ValueContext context) {
           List<String> strings = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            return Result.ok(strings);
        }

        @Override
        public String convertToPresentation(List<String> value, ValueContext context) {
            return String.join(", ", value);
        }
    }

    private static boolean isParameterizedListOfType(Type type, Class<?> elementType) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length == 1 && typeArguments[0] == elementType) {
                return true;
            }
        }
        return false;
    }
}
