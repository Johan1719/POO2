package com.laptitefrance.delivery.repositories;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de la capa de repositorios (patrón Repository/DAO).
 *
 * Define las operaciones CRUD comunes a cualquier entidad, parametrizadas por:
 *   - T  : el tipo de la entidad del dominio (Pedido, Cliente, Producto...).
 *   - ID : el tipo de su identificador (en este proyecto, normalmente String).
 *
 * ¿Por qué una interfaz? Permite que los controladores dependan de esta ABSTRACCIÓN
 * y no de una clase concreta. Así se puede intercambiar la implementación (SQL Server,
 * un mock de pruebas, otra BD) sin tocar la lógica de negocio. (Inversión de Dependencias.)
 *
 * findById devuelve Optional para representar de forma segura "puede que no exista",
 * evitando NullPointerException.
 */
public interface IRepositorioBase<T, ID> {

    /**
     * Crear: persiste una entidad nueva en el almacenamiento.
     * @param entity entidad de tipo T a guardar.
     */
    void insert(T entity);

    /**
     * Leer uno: busca una entidad por su identificador.
     * @param id identificador de tipo ID.
     * @return Optional&lt;T&gt; con la entidad si existe, o Optional.empty() si no se encontró.
     */
    Optional<T> findById(ID id);

    /**
     * Leer todos.
     * @return List&lt;T&gt; con todas las entidades; lista vacía si no hay ninguna.
     */
    List<T> findAll();

    /**
     * Actualizar: persiste los cambios de una entidad ya existente (identificada por su ID interno).
     * @param entity entidad de tipo T con los datos actualizados.
     */
    void update(T entity);

    /**
     * Eliminar por identificador.
     * @param id identificador de tipo ID de la entidad a borrar.
     */
    void deleteById(ID id);
}

