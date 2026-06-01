package com.laptitefrance.delivery.models;

public class Categoria {
    private String codCat; // CHAR(6)
    private String nombreCat; // VARCHAR(30)

    public Categoria() {
    }

    public Categoria(String codCat, String nombreCat) {
        this.codCat = codCat;
        this.nombreCat = nombreCat;
    }

    public String getCodCat() {
        return codCat;
    }

    public void setCodCat(String codCat) {
        this.codCat = codCat;
    }

    public String getNombreCat() {
        return nombreCat;
    }

    public void setNombreCat(String nombreCat) {
        this.nombreCat = nombreCat;
    }
}

