package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Producto;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.ProductoRepository;
import com.laptitefrance.delivery.repositories.ProductoRepositoryPagination;



public class ProductoController {

    private final IRepositorioBase<Producto, String> productoRepository;

    private final CategoriaController categoriaController;

    private static final int DEFAULT_PAGE_SIZE = 10;


    public ProductoController() {
        this(new ProductoRepository(), new CategoriaController());
    }



    public ProductoController(IRepositorioBase<Producto, String> productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository);
        this.categoriaController = new CategoriaController();
    }

    public ProductoController(IRepositorioBase<Producto, String> productoRepository, CategoriaController categoriaController) {
        this.productoRepository = Objects.requireNonNull(productoRepository);
        this.categoriaController = Objects.requireNonNull(categoriaController);
    }




    public List<Producto> obtenerProductosConStock() {
        return productoRepository.findAll().stream()
                .filter(p -> p != null)
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());
    }

    public List<Producto> listarProductos(String filtro) {
        String f = filtro == null ? "" : filtro.trim();
        return listarProductosPaginado(f, 1, Integer.MAX_VALUE);
    }

    public List<Producto> listarProductosPaginado(String filtro, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);

        // Paginación real en BD (SQL Server con OFFSET/FETCH)
        return ProductoRepositoryPagination.listarProductosPaginado(filtro, p, ps);
    }


    public int contarProductosFiltrados(String filtro) {
        String f = filtro == null ? "" : filtro.trim();
        return (int) productoRepository.findAll().stream()
                .filter(pObj -> pObj != null)
                .filter(pObj -> {
                    if (f.isEmpty()) return true;
                    String cod = pObj.getCodProducto() == null ? "" : pObj.getCodProducto();
                    String nom = pObj.getNombreProd() == null ? "" : pObj.getNombreProd();
                    return cod.equalsIgnoreCase(f) || nom.toLowerCase().contains(f.toLowerCase());
                })
                .count();
    }

    public void actualizarStock(String codProducto, int nuevoStock) {
        String cod = codProducto == null ? "" : codProducto.trim();
        if (cod.isEmpty()) {
            throw new ValidationException("Código de producto inválido.");
        }
        if (nuevoStock < 0) {
            throw new ValidationException("El stock no puede ser negativo.");
        }

        Producto p = productoRepository.findById(cod)
                .orElseThrow(() -> new ValidationException("No existe el producto con código: " + cod));

        p.setStock((short) nuevoStock);
        productoRepository.update(p);
    }

    public void actualizarPrecio(String codProducto, double nuevoPrecio) {
        String cod = codProducto == null ? "" : codProducto.trim();
        if (cod.isEmpty()) {
            throw new ValidationException("Código de producto inválido.");
        }
        if (nuevoPrecio < 0) {
            throw new ValidationException("El precio no puede ser negativo.");
        }

        Producto p = productoRepository.findById(cod)
                .orElseThrow(() -> new ValidationException("No existe el producto con código: " + cod));

        p.setPrecioProd(nuevoPrecio);
        productoRepository.update(p);
    }

    public void actualizarNombre(String codProducto, String nuevoNombre) {
        String cod = codProducto == null ? "" : codProducto.trim();
        String nombre = nuevoNombre == null ? "" : nuevoNombre.trim();

        if (cod.isEmpty()) {
            throw new ValidationException("Código de producto inválido.");
        }
        if (nombre.isEmpty()) {
            throw new ValidationException("Ingrese el nombre del producto.");
        }
        if (nombre.length() > 30) {
            throw new ValidationException("El nombre es muy largo (Máximo 30 caracteres). ");
        }

        Producto p = productoRepository.findById(cod)
                .orElseThrow(() -> new ValidationException("No existe el producto con código: " + cod));

        p.setNombreProd(nombre);
        productoRepository.update(p);
    }

    public void actualizarActivo(String codProducto, boolean activo) {
        String cod = codProducto == null ? "" : codProducto.trim();
        if (cod.isEmpty()) {
            throw new ValidationException("Código de producto inválido.");
        }

        Producto p = productoRepository.findById(cod)
                .orElseThrow(() -> new ValidationException("No existe el producto con código: " + cod));

        p.setActivo(activo);
        productoRepository.update(p);
    }

    public void crearProducto(String nombreProd, int stock, double precioProd, String codCat, boolean activo) {
        String nom = nombreProd == null ? "" : nombreProd.trim();
        String cod = codCat == null ? "" : codCat.trim();

        if (nom.isEmpty()) {
            throw new ValidationException("Ingrese el nombre del producto.");
        }
        if (nom.length() > 30) {
            throw new ValidationException("El nombre es muy largo (Máximo 30 caracteres).");
        }
        if (stock < 0) {
            throw new ValidationException("El stock no puede ser negativo.");
        }
        if (precioProd < 0) {
            throw new ValidationException("El precio no puede ser negativo.");
        }
        if (cod.isEmpty()) {
            throw new ValidationException("Seleccione una categoría.");
        }

        // Validar que la categoría exista
        boolean existeCat = categoriaController.listarCategorias().stream()
                .anyMatch(c -> c != null && cod.equalsIgnoreCase(c.getCodCat()));
        if (!existeCat) {
            throw new ValidationException("La categoría seleccionada no existe.");
        }

        Producto nuevo = new Producto();

        // CodProducto es PK NOT NULL en BD. Generamos el código aquí para evitar NULL.
        // Formato esperado: PR0001, PR0002, ...
        // BD: CodProducto CHAR(5). Ej: PR001, PR002 ... (2 letras + 3 dígitos)
        String nuevoCod = "PR" + String.format("%03d", Math.abs((int) (Math.random() * 1000)));

        nuevo.setCodProducto(nuevoCod);

        nuevo.setNombreProd(nom);
        nuevo.setStock((short) stock);
        nuevo.setPrecioProd(precioProd);
        nuevo.setCodCat(cod);
        nuevo.setActivo(activo);

        productoRepository.insert(nuevo);
    }

    public void eliminarProducto(String codProducto) {
        String cod = codProducto == null ? "" : codProducto.trim();
        if (cod.isEmpty()) {
            throw new ValidationException("Código de producto inválido.");
        }

        productoRepository.findById(cod)
                .orElseThrow(() -> new ValidationException("No existe el producto con código: " + cod));

        productoRepository.deleteById(cod);
    }

}



