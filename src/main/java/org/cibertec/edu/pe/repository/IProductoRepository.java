package org.cibertec.edu.pe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cibertec.edu.pe.model.Producto;

public interface IProductoRepository extends JpaRepository<Producto, Integer> {
	
	
	
}
