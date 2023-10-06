package org.cibertec.edu.pe.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cibertec.edu.pe.model.Detalle;
import org.cibertec.edu.pe.model.Producto;
import org.cibertec.edu.pe.repository.IDetalleRepository;
import org.cibertec.edu.pe.repository.IProductoRepository;
import org.cibertec.edu.pe.repository.IVentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
@SessionAttributes({"carrito", "total"})
public class ProductoController {
	@Autowired
	private IProductoRepository productoRepository;
	@Autowired
	private IVentaRepository ventaRepository;
	@Autowired
	private IDetalleRepository detalleRepository;
	
	@GetMapping("/index")
	public String listado(Model model) {
		List<Producto> lista = new ArrayList<>();
		lista = productoRepository.findAll();
		model.addAttribute("productos", lista);
		return "index";
	}
	//AGREGAR AL CARRITO
	@GetMapping("/agregar/{idProducto}")
	public String agregar(Model model, @PathVariable(name = "idProducto", required = true) int idProducto) {
		// Obtén el producto que deseas agregar al carrito desde el repositorio
	    Producto producto = productoRepository.findById(idProducto).orElse(null);

	    if (producto != null) {
	        // Obtén el carrito de la sesión
	        List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");

	        // Verifica si el producto ya está en el carrito
	        boolean productoEnCarrito = false;
	        for (Detalle detalle : carrito) {
	            if (detalle.getIdProducto() == idProducto) {
	                // Si el producto ya está en el carrito, incrementa la cantidad
	                detalle.setCantidad(detalle.getCantidad() + 1);
	                detalle.setSubtotal(detalle.getCantidad() * detalle.getPrecio());
	                productoEnCarrito = true;
	                break;
	            }
	        }

	        if (!productoEnCarrito) {
	            // Si el producto no está en el carrito, crea un nuevo detalle
	            Detalle nuevoDetalle = new Detalle();
	            nuevoDetalle.setIdProducto(idProducto);
	            nuevoDetalle.setDescripcion(producto.getDescripcion());
	            nuevoDetalle.setImagen(producto.getImagen());
	            nuevoDetalle.setPrecio(producto.getPrecio());
	            nuevoDetalle.setCantidad(1);
	            nuevoDetalle.setSubtotal(producto.getPrecio());

	            // Agrega el nuevo detalle al carrito
	            carrito.add(nuevoDetalle);
	        }

	        // Actualiza el carrito en la sesión
	        model.addAttribute("carrito", carrito);
	    }

	    // Redirige de nuevo a la página de inicio
	    return "redirect:/index";
	}
	
	@GetMapping("/carrito")
	public String carrito() {
		return "carrito";
	}
	@GetMapping("/create")
	public String create() {
		return "create";
	}
	
	@GetMapping("/pagar")
	public String pagar(Model model) {
	    // Codigo para pagar
	    return "mensaje";
	}

	
	//ACTUALIZAR CARRITO COMPRAS
	@PostMapping("/actualizarCarrito")
    public String actualizarCarrito(@ModelAttribute("carrito") List<Detalle> carrito, Model model) {
        // No es necesario obtener el carrito de la sesión, ya que lo estamos pasando como atributo del modelo

        // No necesitas iterar a través de los detalles del carrito aquí, ya que los datos se han vinculado automáticamente
		System.out.println("Carrito recibido: " + carrito.toString());
        // Actualiza el carrito en la sesión (esto ya se hace automáticamente debido a la anotación @ModelAttribute)

        // Redirige de nuevo a la página del carrito
        return "carrito";
    }
	
	// Inicializacion de variables de la sesion
	@ModelAttribute("carrito")
	public List<Detalle> getCarrito() {
		return new ArrayList<Detalle>();
	}
	
	@ModelAttribute("total")
	public double getTotal() {
		return 0.0;
	}

	//CREAR PRODUCTO NUEVO
	@PostMapping("/guardarProducto")
    public String guardarProducto(@RequestParam("descripcion") String descripcion,
                                  @RequestParam("precio") double precio,
                                  @RequestParam("stock") int stock,
                                  @RequestParam("imagen") MultipartFile imagen) {

		// Crear una nueva instancia de Producto y establecer sus propiedades
	    Producto nuevoProducto = new Producto();
	    nuevoProducto.setDescripcion(descripcion);
	    nuevoProducto.setPrecio(precio);
	    nuevoProducto.setStock(stock);

	    try {
	        // Obtener el contenido de la imagen y guardarla en una ubicación específica
	        byte[] imagenBytes = imagen.getBytes();
	     // Generar un nombre único para la imagen utilizando UUID
	        String nombreArchivo = UUID.randomUUID().toString() + "." + obtenerExtension(imagen.getOriginalFilename());

	        String rutaImagen = "src/main/resources/static/img/" + nombreArchivo; // Ruta donde deseas guardar la imagen

	        // Guardar la imagen en el sistema de archivos
	        Path rutaDestino = Paths.get(rutaImagen);
	        Files.write(rutaDestino, imagenBytes);

	        // Establecer la ruta de la imagen en el objeto Producto
	        nuevoProducto.setImagen(nombreArchivo);

	        // Guardar el producto en la base de datos
	        productoRepository.save(nuevoProducto);
	    } catch (IOException e) {
	        // Manejar errores de carga de archivos
	        e.printStackTrace();
	    }

	    // Redirigir a la página de inicio u otra página deseada
	    return "redirect:/index";
    }
	//PARA CONVERTIR LA IMAGEN Y GUARDAR EN LA CARPETA IMG
	private String obtenerExtension(String nombreArchivoOriginal) {
	    String extension = "";
	    int lastIndex = nombreArchivoOriginal.lastIndexOf(".");
	    if (lastIndex >= 0) {
	        extension = nombreArchivoOriginal.substring(lastIndex + 1);
	    }
	    return extension;
	}
}
