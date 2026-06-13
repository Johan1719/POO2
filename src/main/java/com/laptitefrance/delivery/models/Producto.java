package com.laptitefrance.delivery.models;

public class Producto {
    private String codProducto; // CHAR(5)
    private String nombreProd; // VARCHAR(30) UNIQUE
    private Short stock; // SMALLINT
    private double precioProd; // FLOAT
    private String codCat; // FK -> Categoria(CodCat)
    private boolean activo; // BIT


    public Producto() {
    }

    public Producto(String codProducto, String nombreProd, Short stock, double precioProd, String codCat) {
        this.codProducto = codProducto;
        this.nombreProd = nombreProd;
        this.stock = stock;
        this.precioProd = precioProd;
        this.codCat = codCat;
        this.activo = true;
    }

    public Producto(String codProducto, String nombreProd, Short stock, double precioProd, String codCat, boolean activo) {
        this.codProducto = codProducto;
        this.nombreProd = nombreProd;
        this.stock = stock;
        this.precioProd = precioProd;
        this.codCat = codCat;
        this.activo = activo;
    }


    public String getCodProducto() {
        return codProducto;
    }

    public void setCodProducto(String codProducto) {
        this.codProducto = codProducto;
    }

    public String getNombreProd() {
        return nombreProd;
    }

    public void setNombreProd(String nombreProd) {
        this.nombreProd = nombreProd;
    }

    public Short getStock() {
        return stock;
    }

    public void setStock(Short stock) {
        this.stock = stock;
    }

    public double getPrecioProd() {
        return precioProd;
    }

    public void setPrecioProd(double precioProd) {
        this.precioProd = precioProd;
    }

    public String getCodCat() {
        return codCat;
    }

    public void setCodCat(String codCat) {
        this.codCat = codCat;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}


