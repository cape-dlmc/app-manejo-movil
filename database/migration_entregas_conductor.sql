-- Migración 2: distingue gastos pagados con dinero recaudado de los pagados por la empresa.
-- Sección 12.2 del documento: un gasto pagado por la empresa no se descuenta de la entrega.
-- Ejecutar una sola vez sobre bases de datos creadas antes de esta versión.
ALTER TABLE movimientos_financieros
    ADD COLUMN IF NOT EXISTS pagado_con_recaudo BOOLEAN NOT NULL DEFAULT TRUE;
