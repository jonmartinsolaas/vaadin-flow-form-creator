# Vaadin Flow form creator

Before you read any further, also check out git@github.com:mstahv/autoform-demo.git

This project will take a given Layout, Binder and Bean, and create 
Vaadin fields on the layout, according to some annotationson the bean.
Using a FormLayout the @FieldWidth annotation has noeffect.

@FieldOrder annotation is not optional, if it is left out it means the
field will not be generated on the form.

@FieldType can be used to override resolving of field type.

If your enum implements HasLabel the getLabel will be used to populate
the combobox.

Lists in the bean, of type string or number, can be represented in gui
as a comma separated list in a TextField or NumberField.

There is no registered Maven component for this code at the moment,
and I'm not sure there will be. Neither is the code well tested or to 
be considered production ready. For now, consider it a prototype itself,
that could be useful in application GUI prototyping.

GUI stuff always involve tweaking and there will always be requirements
no one else has thought of. Normally I get there using Alejandro Duartes
excellent CRUD Ui addon, but sometimes it is overkill, or does not do 
what I want exactly. So consider this code a starting point and modify it
to get where you need to go. Feel free to contribute your modifications. 
Maybe they even could be useful in CRUD Ui.

Example code:

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSummary {

    @NotNull(message="Year is mandatory")
    @Max(value = 2050, message = "Year must be less than 2051")
    @Min(value = 2010, message = "Year must be larger than 2009")
    @FieldLabel(value="The year it all started")
    @FieldOrder(1)
    @FieldWidth("100px")
    @FieldType(NumberField.class)
    Short year;
    
    @FieldOrder(2)
    @FieldLabel("Project type")
    ProjectTypeEnum projectType; 
    // todo: have enum String values used as values in the combobox, looks nicer.
    
    ...
```

Usage:

```java
...

Div parent = new Div();
ProjectSummary summary = new ProjectSummary();
FormLayout formLayout = new FormLayout(); // Can use VerticalLayout etc. as well.
BeanValidationBinder<ProjectSummary> binder 
        = new BeanValidationBinder<>(ProjectSummary.class);

FormCreator.bindAndCreateFields(formLayout, binder, params);

Button button = new Button();

button.addClickListener(e -> {
    binder.writeBean(summary);
    System.out.println(summary.toString());
});

...        

parent.add(formLayout);

```
