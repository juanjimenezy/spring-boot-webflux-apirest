package com.bolsadeideas.springboot.webflux.app.controllers;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Value("${config.uploads.path}")
    private String path;

    @PostMapping("/upload/{id}")
    public Mono<ResponseEntity<Producto>> upload(@PathVariable String id, @RequestPart FilePart file) {
        return productoService.findById(id).flatMap(p -> {
                    p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", ""));

                    return file.transferTo(new File(path + p.getFoto())).then(productoService.save(p));
                }).map(p -> ResponseEntity.ok(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    private Mono<ResponseEntity<Flux<Producto>>> findAll() {
        return Mono.just(new ResponseEntity<>(productoService.findAll(), HttpStatus.OK));
    }

    @GetMapping("/{id}")
    private Mono<ResponseEntity<Producto>> findById(@PathVariable String id) {
        /*return productoService.findById(id).map(p -> {
            return new ResponseEntity<>(p, HttpStatus.OK);
        });*/
        return productoService.findById(id)
                .map(p -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(p))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Mono<ResponseEntity<Producto>> save(@RequestBody Producto producto) {
        if (producto.getCreateAt() == null) {
            producto.setCreateAt(new Date());
        }
        return productoService.save(producto).map(p -> {
            return ResponseEntity
                    .created(URI.create("/api/productos/".concat(p.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(p);
        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Producto>> update(@RequestBody Producto producto, @PathVariable String id) {
        return productoService.findById(id).flatMap(p -> {
            p.setNombre(producto.getNombre().toUpperCase());
            p.setPrecio(producto.getPrecio());
            p.setCreateAt(producto.getCreateAt());
            p.setCategoria(producto.getCategoria());
            return productoService.save(p);
        }).map(p -> {
            return ResponseEntity
                    .created(URI.create("/api/productos/".concat(p.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(p);
        }).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id) {
        return productoService.findById(id).flatMap(p -> {
            return productoService.delete(p).then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
        }).defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }

}
