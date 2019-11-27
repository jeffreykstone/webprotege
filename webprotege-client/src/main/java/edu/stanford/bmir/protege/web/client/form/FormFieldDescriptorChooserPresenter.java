package edu.stanford.bmir.protege.web.client.form;

import com.google.common.collect.ImmutableList;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.shared.form.field.FormFieldDescriptor;
import edu.stanford.bmir.protege.web.shared.form.field.GridFieldDescriptor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-11-26
 */
public class FormFieldDescriptorChooserPresenter {

    @Nonnull
    private final FormFieldDescriptorChooserView view;

    @Nonnull
    private final NoFieldDescriptorView noFieldDescriptorView;

    @Nonnull
    private final ImmutableList<FormFieldDescriptorPresenterFactory> fieldPresenterFactories;

    @Nonnull
    private final DispatchServiceManager dispatchServiceManager;

    @Nonnull
    private final Map<String, FormFieldDescriptorPresenter> fieldType2FieldPresenter = new HashMap<>();

    @Nonnull
    private final Map<String, FormFieldDescriptor> fieldType2FieldDescriptor = new HashMap<>();

    @Nonnull
    private Optional<FormFieldDescriptorPresenter> currentFieldPresenter = Optional.empty();

    @Inject
    public FormFieldDescriptorChooserPresenter(@Nonnull FormFieldDescriptorChooserView view,
                                               @Nonnull NoFieldDescriptorView noFieldDescriptorView,
                                               @Nonnull ImmutableList<FormFieldDescriptorPresenterFactory> fieldPresenterFactories,
                                               @Nonnull DispatchServiceManager dispatchServiceManager) {
        this.view = view;
        this.noFieldDescriptorView = noFieldDescriptorView;
        this.fieldPresenterFactories = fieldPresenterFactories;
        this.dispatchServiceManager = dispatchServiceManager;
    }

    public void start(@Nonnull AcceptsOneWidget container) {
        container.setWidget(view);
        fieldPresenterFactories.forEach(factory -> {
            view.addAvailableFieldType(factory.getDescriptorType(), factory.getDescriptorLabel());
        });
        view.setFieldTypeChangedHandler(this::handleFieldTypeChanged);
        handleFieldTypeChanged();
    }

    private void handleFieldTypeChanged() {
        currentFieldPresenter.map(FormFieldDescriptorPresenter::getFormFieldDescriptor)
                             .ifPresent(this::cacheFieldDescriptor);

        String fieldType = view.getFieldType();
        FormFieldDescriptor nextDescriptor = fieldType2FieldDescriptor.get(fieldType);
        if(nextDescriptor != null) {
            setFormFieldDescriptor(nextDescriptor);
        }
        else {
            fieldPresenterFactories.stream()
                                   .filter(factory -> factory.getDescriptorType()
                                                             .equals(fieldType))
                                   .findFirst()
                                   .map(FormFieldDescriptorPresenterFactory::createDefaultDescriptor)
                                   .ifPresent(this::setFormFieldDescriptor);
        }

    }

    public Optional<FormFieldDescriptor> getFormFieldDescriptor() {
        return currentFieldPresenter.map(FormFieldDescriptorPresenter::getFormFieldDescriptor);
    }

    private void cacheFieldDescriptor(FormFieldDescriptor descriptor) {
        fieldType2FieldDescriptor.put(descriptor.getAssociatedType(), descriptor);
    }

    public void setFormFieldDescriptor(FormFieldDescriptor formFieldDescriptor) {
        String type = formFieldDescriptor.getAssociatedType();
        view.setFieldType(type);
        Optional<FormFieldDescriptorPresenter> fieldPresenter = getOrCreateFieldPresenter(type);
        fieldPresenter.ifPresent(p -> {
            p.start(view.getFieldEditorContainer());
            p.setFormFieldDescriptor(formFieldDescriptor);
            this.currentFieldPresenter = Optional.of(p);
        });
        if(!fieldPresenter.isPresent()) {
            currentFieldPresenter = Optional.empty();
            view.getFieldEditorContainer().setWidget(noFieldDescriptorView);
        }
    }


    @Nonnull
    private Optional<FormFieldDescriptorPresenter> getOrCreateFieldPresenter(String type) {
        FormFieldDescriptorPresenter presenter = fieldType2FieldPresenter.get(type);
        if(presenter != null) {
            return Optional.of(presenter);
        }
        Optional<FormFieldDescriptorPresenter> presenterForType = getPresenterForType(type);
        presenterForType
                .ifPresent(p -> {
                    fieldType2FieldPresenter.put(type, p);
                    fieldType2FieldDescriptor.put(type, p.getFormFieldDescriptor());
                });
        return presenterForType;
    }

    private Optional<FormFieldDescriptorPresenter> getPresenterForType(String type) {
        return fieldPresenterFactories.stream()
                                      .filter(factory -> factory.getDescriptorType()
                                                                .equals(type))
                                      .map(FormFieldDescriptorPresenterFactory::create)
                                      .findFirst();
    }
}