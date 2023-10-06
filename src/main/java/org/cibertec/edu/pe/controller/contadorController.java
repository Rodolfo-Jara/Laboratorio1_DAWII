package org.cibertec.edu.pe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes({"contador"})
public class contadorController {
  @ModelAttribute("contador")
  
  public int getContador() {
    return 5;	// Valor Inicial
  }
  
  @RequestMapping("/contador")
  public String verContador() {
    return "contador";
  }
  
  @RequestMapping("/contadorincrementar")
  public String incrementar(Model modelo) {
    int contador=(int) modelo.getAttribute("contador");
    contador++;
    modelo.addAttribute("contador",contador);
    return "contador";
  }
}
