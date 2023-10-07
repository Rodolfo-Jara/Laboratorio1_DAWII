package org.cibertec.edu.pe.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.cibertec.edu.pe.model.Detalle;
import org.cibertec.edu.pe.model.Producto;
import org.cibertec.edu.pe.model.Venta;
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
@SessionAttributes({ "carrito", "total" })
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

	// AGREGAR AL CARRITO
	@GetMapping("/agregar/{idProducto}")
	public String agregar(Model model, @PathVariable(name = "idProducto", required = true) int idProducto) {
		Producto producto = productoRepository.findById(idProducto).orElse(null);

		if (producto != null) {
			List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
			boolean productoEnCarrito = false;
			for (Detalle detalle : carrito) {
				if (detalle.getIdProducto() == idProducto) {
					detalle.setCantidad(detalle.getCantidad() + 1);
					detalle.setSubtotal(detalle.getCantidad() * detalle.getPrecio());
					productoEnCarrito = true;
					break;
				}
			}
			if (!productoEnCarrito) {
				Detalle nuevoDetalle = new Detalle();
				nuevoDetalle.setIdProducto(idProducto);
				nuevoDetalle.setDescripcion(producto.getDescripcion());
				nuevoDetalle.setImagen(producto.getImagen());
				nuevoDetalle.setPrecio(producto.getPrecio());
				nuevoDetalle.setCantidad(1);
				nuevoDetalle.setSubtotal(producto.getPrecio());
				carrito.add(nuevoDetalle);
			}
			model.addAttribute("carrito", carrito);
		}
		return "redirect:/index";
	}

	
	// REALIZAR PAGO Y REGISTRAR EN EL DETALLE
	@GetMapping("/pagar")
	public String realizarPagoYRegistrar(HttpSession session, Model model) {
		List<Detalle> cart = (List<Detalle>) session.getAttribute("carrito");
		double total = (double) session.getAttribute("total");
		Venta venta = new Venta();
		venta.setFechaRegistro(new Date());
		venta.setMontoTotal(total);

		ventaRepository.save(venta);
		for (Detalle detalle : cart) {
			detalle.setIdVenta(venta.getIdVenta());
			detalleRepository.save(detalle);
		}
		session.setAttribute("carrito", cart);
		session.setAttribute("total", total);

		// MENSAJE
		String mensaje = "PAGO REGISTRADO CON EXITO";
		model.addAttribute("mensaje", mensaje);

		return "carrito";
	}

	// ACTUALIZAR RL CARRITO
	@PostMapping("/actualizarCarrito")
	public String actualizarCarrito(HttpServletRequest request, HttpSession session, Model model) {
		List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");
		boolean validData = true; 
		double subtotal = 0.0;
		double precioEnvio = 0.0;
		double descuento = 0.0;

		for (Detalle detalle : carrito) {
			String cantidadParam = request.getParameter("cantidad_" + detalle.getIdProducto());
			int cantidad = detalle.getCantidad();

			if (cantidadParam != null && !cantidadParam.isEmpty()) {
				try {
					cantidad = Integer.parseInt(cantidadParam);
					detalle.setCantidad(cantidad);
					detalle.setSubtotal(detalle.getPrecio() * cantidad);
				} catch (NumberFormatException e) {
					validData = false; 
				}
			}

			subtotal += detalle.getSubtotal();
			precioEnvio += calcularPrecioEnv(detalle); 
		}

		String precioEnvioParam = request.getParameter("precioEnvio");
		String descuentoParam = request.getParameter("descuento");

		
		try {
			precioEnvio = Double.parseDouble(precioEnvioParam.replace("S/ ", ""));
			descuento = Double.parseDouble(descuentoParam.replace("S/ ", ""));
		} catch (NumberFormatException e) {
			validData = false;
		}

		if (validData) {
			
			double total = subtotal + precioEnvio - descuento;
			session.setAttribute("carrito", carrito);
			session.setAttribute("total", total);

			model.addAttribute("subtotal", subtotal);
			model.addAttribute("precioEnvio", precioEnvio);
			model.addAttribute("descuento", descuento);
			model.addAttribute("total", total);
		}
		return "carrito";
	}
	private double calcularPrecioEnv(Detalle detalle) {
		double precioEnvio = 0.0;
		return precioEnvio;
	}
	@ModelAttribute("total")
	public double getTotal() {
		return 0.0;
	}
	// CREAR PRODUCTO NUEVO
	@GetMapping("/create")
	public String create() {
		return "create";
	}

	@PostMapping("/guardarProducto")
	public String guardarProducto(@RequestParam("descripcion") String descripcion,
			@RequestParam("precio") double precio, @RequestParam("stock") int stock,
			@RequestParam("imagen") MultipartFile imagen) {

		Producto nuevoProducto = new Producto();
		nuevoProducto.setDescripcion(descripcion);
		nuevoProducto.setPrecio(precio);
		nuevoProducto.setStock(stock);

		try {

			byte[] imagenBytes = imagen.getBytes();

			String nombreArchivo = UUID.randomUUID().toString() + "." + obtenerExtension(imagen.getOriginalFilename());

			String rutaImagen = "src/main/resources/static/img/" + nombreArchivo;

			Path rutaDestino = Paths.get(rutaImagen);
			Files.write(rutaDestino, imagenBytes);

			nuevoProducto.setImagen(nombreArchivo);

			productoRepository.save(nuevoProducto);
		} catch (IOException e) {

			e.printStackTrace();
		}

		return "redirect:/index";
	}

	// PARA CONVERTIR LA IMAGEN Y GUARDAR EN LA CARPETA IMG
	private String obtenerExtension(String nombreArchivoOriginal) {
		String extension = "";
		int lastIndex = nombreArchivoOriginal.lastIndexOf(".");
		if (lastIndex >= 0) {
			extension = nombreArchivoOriginal.substring(lastIndex + 1);
		}
		return extension;
	}
	// PARA ELIMINAR UN PRODUCTO DESPUES DE AGREGAR AL CARRITO
	@GetMapping("/eliminar/{idProducto}")
	public String eliminar(Model model, @PathVariable(name = "idProducto", required = true) int idProducto,
			HttpSession session) {
		List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");
		if (carrito != null) {
			carrito.removeIf(detalle -> detalle.getIdProducto() == idProducto);
			session.setAttribute("carrito", carrito);

			// MENSAJE
			String mensaje = "Producto eliminado correctamente.";
			model.addAttribute("mensaje", mensaje);
		}
		return "redirect:/carrito";
	}
	// PARA LISTAR EL EL CARRITO
	@ModelAttribute("carrito")
	public List<Detalle> getCarrito() {
		return new ArrayList<Detalle>();
	}
	@GetMapping("/carrito")
	public String carrito(Model model, HttpSession session) {

		List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");
		double subtotal = calcularSubtotal(carrito);
		double precioEnvio = calcularPrecioEnvio(carrito);
		double descuento = calcularDescuento(carrito);
		double total = subtotal + precioEnvio - descuento;

		model.addAttribute("subtotal", subtotal);
		model.addAttribute("precioEnvio", precioEnvio);
		model.addAttribute("descuento", descuento);
		model.addAttribute("total", total);

		return "carrito";
	}
	// METODOS PARA CALCULAR LAS OPERACIONES
	private double calcularSubtotal(List<Detalle> carrito) {
		double subtotal = 0.0;
		for (Detalle detalle : carrito) {
			subtotal += detalle.getSubtotal();
		}
		return subtotal;
	}
	//
	private double calcularPrecioEnvio(List<Detalle> carrito) {
		return 0.0;
	}

	private double calcularDescuento(List<Detalle> carrito) {
		return 0.0;
	}
}
