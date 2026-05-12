package com.admin.adminlfarma_mini.repository;

import com.admin.adminlfarma_mini.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findFirstByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRol(String rol);

    boolean existsByRol(String rol);

    // Buscar usuarios activos por rol
    List<Usuario> findByRolAndActivoTrue(String rol);

    // Buscar todos los usuarios activos
    List<Usuario> findByActivoTrue();

    // Buscar usuarios por rol ignorando mayúsculas
    @Query("SELECT u FROM Usuario u WHERE UPPER(u.rol) LIKE UPPER(CONCAT('%', :rol, '%')) AND u.activo = true")
    List<Usuario> findUsuariosByRolContaining(@Param("rol") String rol);
}