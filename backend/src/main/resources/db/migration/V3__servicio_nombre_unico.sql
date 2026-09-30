-- V3: no puede haber dos servicios activos con el mismo nombre en la misma barbería (issue #22)
CREATE UNIQUE INDEX uq_servicio_nombre_activo
    ON servicio (barberia_id, lower(nombre))
    WHERE activo;