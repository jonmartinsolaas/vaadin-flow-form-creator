# Vaadin Flow form creator

This project will take a given Layout, Binder and Bean, and 
create Vaadin fields on the form, according to some annotations
on the bean. 

@FieldOrder annotation is not optional, if it is left out it means
the field will not be generated on the form.

@FieldType can be used to override the default field type deduction.

You don't have to use Lombok annotations.

Example code

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Parameters {

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

Usage

```java
...

Div parent = new Div();
Parameters params = new Parameters();
FormLayout formLayout = new FormLayout(); // Can use VerticalLayout etc. as well.
BeanValidationBinder<UploadDocumentParams> binder 
        = new BeanValidationBinder<>(UploadDocumentParams.class);

FormCreator.bindAndCreateFields(formLayout, binder, params);

Button button = new Button();

button.addClickListener( e -> {
    binder.writeBean(params);
    System.out.println(params.toString());
});

...        

parent.add(formLayout);

```