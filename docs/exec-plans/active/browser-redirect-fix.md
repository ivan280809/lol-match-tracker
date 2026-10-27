# Primer POST del dashboard

La prueba en navegador reproduce: POST /players devuelve un Location con
`;jsessionid`, y el GET posterior responde 404 en la raiz. MockMvc no seguia ese
redirect con un servidor servlet real.

Se conserva el estado flash mediante cookies de sesion y se deshabilita la
reescritura de identificadores en URLs. No se crean sesiones anticipadas ni se
amplian rutas para aceptar URLs con credenciales de sesion. Se prueba con un
cliente HTTP y un cookie jar nuevos: POST, Location sin identificador y GET 200
con confirmacion y jugador creado. Repetir navegador, Maven y CI con PostgreSQL.

Detalle, edicion y auditoria reutilizan el icono inline del dashboard para
evitar la peticion fallida del navegador a /favicon.ico.
