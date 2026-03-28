Cypress.Commands.add('selectDateTimePickerDate', () => {
  cy.get(
    '.datetimepicker > .datepicker > .datepicker-buttons-container > .datepicker-button > .datepicker-button-content'
  )
    .first()
    .click({force: true});
});

// vue-ctk-date-time-picker: open by id prefix, pick Nth enabled day, confirm.
Cypress.Commands.add('pickCtkDateTimeDay', (idPrefix, dayIndex) => {
  cy.get(`#${idPrefix}-input`).should('be.visible').click({ force: true });
  cy.get(`#${idPrefix}-wrapper`).should('be.visible');
  cy.get(`#${idPrefix}-wrapper button.datepicker-day:not(:disabled)`)
    .eq(dayIndex)
    .click({ force: true });
  cy.get(`#${idPrefix}-wrapper .datepicker-button.validate`).click({ force: true });
});


