package net.tradeshift.treeapp.exceptions;

public class RequiredFieldException extends RuntimeException {

    private final String fieldName;

    public RequiredFieldException(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }
}
