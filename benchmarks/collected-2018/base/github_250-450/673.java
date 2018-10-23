// https://searchcode.com/api/result/93398803/

/**
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * $Id$ Mueble.java
 * Universidad de los Andes (Bogota - Colombia)
 * Departamento de Ingenieria de Sistemas y Computacion
 * Licenciado bajo el esquema Academic Free License version 3.0
 *
 * Ejercicio: Muebles de los Alpes
 * Autor: Juan Sebastian Urrego
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

package co.edu.uniandes.csw.mueblesdelosalpes.dto;

/**
 * Clase que representa la informacion de un mueble en el sistema
 * @author Juan Sebastian Urrego
 */
public class Mueble
{

    //-----------------------------------------------------------
    // Atributos
    //-----------------------------------------------------------

    /**
     * Referencia que identifica el modelo del mueble en el sistema.
     */
    private long referencia;

    /**
     * Nombre del mueble.
     */
    private String nombre;

    /**
     * Descripcion del mueble.
     */
    private String descripcion;

    /**
     * Tipo de mueble.
     */
    private TipoMueble tipo;

    /**
     * Precio del mueble
     */
    private double precio;

    /**
     * Nombre de la imagen
     */
    private String imagen;

    /**
     * Cantidad de items
     */
    private int cantidad;

    /**
     * Indica si el mueble fue seleccionado
     */
    private boolean seleccion;

    //-----------------------------------------------------------
    // Constructores
    //-----------------------------------------------------------

    /**
     * Constructor sin argumentos de la clase
     */
    public Mueble() 
    {

    }

    /**
     * Constructor de la clase. Inicializa los atributos con los valores que ingresan por parametro.
     * @param referencia Referencia del mueble
     * @param nombre Nombre del mueble
     * @param descripcion Descripion del mueble
     * @param tipo Tipo de mueble
     * @param cantidad Cantidad de ejemplares
     */
    public Mueble(long referencia, String nombre, String descripcion, TipoMueble tipo,int cantidad,String imagen,double precio)
    {
        this.referencia = referencia;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.cantidad=cantidad;
        this.imagen=imagen;
        this.precio=precio;
    }

    //-----------------------------------------------------------
    // Getters y setters
    //-----------------------------------------------------------

    /**
     * Devuelve la descripcion del mueble
     * @return descripcion Descripcion del mueble
     */
    public String getDescripcion()
    {
        return descripcion;
    }

    /**
     * Modifica la descripcion del mueble
     * @param descripcion Nueva descripcion del mueble
     */
    public void setDescripcion(String descripcion)
    {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el nombre del mueble
     * @return nombre Nombre del mueble
     */
    public String getNombre()
    {
        return nombre;
    }

    /**
     * Modifica el nombre del mueble
     * @param nombre Nuevo nombre del mueble
     */
    public void setNombre(String nombre)
    {
        this.nombre = nombre;
    }

    /**
     * Devuelve la referencia del mueble
     * @return referencia Referencia del mueble
     */
    public long getReferencia()
    {
        return referencia;
    }

    /**
     * Modifica la referencia del mueble
     * @param referencia Nueva referencia del mueble
     */
    public void setReferencia(long referencia) {
        this.referencia = referencia;
    }

    /**
     * Devuelve el tipo de mueble
     * @return tipo Tipo de mueble
     */
    public TipoMueble getTipo()
    {
        return tipo;
    }

    /**
     * Modifica el tipo de mueble
     * @param tipo Nuevo tipo de mueble
     */
    public void setTipo(TipoMueble tipo)
    {
        this.tipo = tipo;
    }

    /**
     * Devuelve el estado de seleccion del mueble
     * @return seleccion Verdadero o falso
     */
    public boolean isSeleccion()
    {
        return seleccion;
    }

    /**
     * Cambia el estado de seleccion de un mueble
     * @param seleccion Nuevo estado de seleccion
     */
    public void setSeleccion(boolean seleccion)
    {
        this.seleccion = seleccion;
    }

    /**
     * Devuelve la cantidad de ejemplares de un mueble
     * @return cantidad Cantidad de muebles
     */
    public int getCantidad()
    {
        return cantidad;
    }

    /**
     * Modifica la cantidad de ejemplares de un mueble
     * @param cantidad Nueva cantidad de muebles
     */
    public void setCantidad(int cantidad)
    {
        this.cantidad = cantidad;
    }

    /**
     * Nombre de la imagen
     * @return imagen Nombre de la imagen
     */
    public String getImagen()
    {
        return imagen;
    }

    /**
     * Modifica el nombre de la imagen
     * @param imagen Nuevo nombre de imagen
     */
    public void setImagen(String imagen)
    {
        this.imagen = imagen;
    }

    /**
     * Devuelve el precio del mueble
     * @return precio Precio del mueble
     */
    public double getPrecio()
    {
        return precio;
    }

    /**
     * Modifica el precio del mueble
     * @param precio Nuevo precio del mueble
     */
    public void setPrecio(double precio)
    {
        this.precio = precio;
    }

    //-----------------------------------------------------------
    // Metodos
    //-----------------------------------------------------------

    /**
     * Aumenta la cantidad de muebles
     */
    public void incrementarCantidad()
    {
        cantidad++;
    }

    /**
     * Reduce la cantidad de muebles
     */
    public void reducirCantidad()
    {
        cantidad--;
    } 

}

