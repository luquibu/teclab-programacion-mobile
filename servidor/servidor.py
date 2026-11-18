"""Servicio local para Mis recordatorios. Requiere Python 3."""
import argparse
import json
import sqlite3
from contextlib import closing
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def validar(dato, clave):
    if not isinstance(dato, dict):
        raise ValueError
    if type(dato.get("id")) is not int or dato["id"] != clave:
        raise ValueError
    if not isinstance(dato.get("titulo"), str) or len(dato["titulo"].strip()) < 4:
        raise ValueError
    if dato.get("descripcion") is not None and (
        not isinstance(dato["descripcion"], str) or len(dato["descripcion"]) > 200
    ):
        raise ValueError
    opciones = {"categoria": ("estudio", "entregas", "examenes"),
                "prioridad": ("alta", "media", "baja"), "estado": ("pendiente", "completado")}
    if any(dato.get(campo) not in valores for campo, valores in opciones.items()):
        raise ValueError
    if type(dato.get("fecha")) is not int or dato["fecha"] <= 0:
        raise ValueError
    return {campo: dato.get(campo) for campo in ("id", "titulo", "descripcion", "categoria", "prioridad", "estado", "fecha")}


class Servicio(BaseHTTPRequestHandler):
    def responder(self, estado, dato=None):
        cuerpo = json.dumps(dato, ensure_ascii=False).encode("utf-8") if dato is not None else b""
        self.send_response(estado)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)

    def clave(self):
        partes = self.path.split("/")
        if len(partes) != 3 or partes[1] != "recordatorios" or not partes[2].isascii() or not partes[2].isdigit() or len(partes[2]) > 19:
            return None
        clave = int(partes[2])
        return clave if 0 < clave <= 2**63 - 1 else None

    def do_GET(self):
        if self.path != "/recordatorios":
            return self.responder(404)
        with closing(sqlite3.connect(self.server.archivo)) as db:
            registros = [json.loads(r[0]) for r in db.execute("SELECT contenido FROM recordatorios ORDER BY id")]
        self.responder(200, registros)

    def do_PUT(self):
        clave = self.clave()
        if clave is None:
            return self.responder(404)
        try:
            largo = int(self.headers.get("Content-Length", "0"))
            if largo <= 0:
                return self.responder(400)
            registro = validar(json.loads(self.rfile.read(largo)), clave)
        except (ValueError, UnicodeError):
            return self.responder(400)
        with closing(sqlite3.connect(self.server.archivo)) as db, db:
            db.execute("INSERT INTO recordatorios VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET contenido=excluded.contenido",
                       (clave, json.dumps(registro, ensure_ascii=False)))
        self.responder(200, registro)

    def do_DELETE(self):
        clave = self.clave()
        if clave is None:
            return self.responder(404)
        with closing(sqlite3.connect(self.server.archivo)) as db, db:
            db.execute("DELETE FROM recordatorios WHERE id=?", (clave,))
        self.responder(204)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--puerto", type=int, default=8765)
    parser.add_argument("--archivo", default="recordatorios.sqlite3")
    args = parser.parse_args()
    with closing(sqlite3.connect(args.archivo)) as db, db:
        db.execute("CREATE TABLE IF NOT EXISTS recordatorios(id INTEGER PRIMARY KEY, contenido TEXT NOT NULL)")
    with ThreadingHTTPServer(("127.0.0.1", args.puerto), Servicio) as http:
        http.archivo = args.archivo
        print(f"Servicio en http://127.0.0.1:{args.puerto}/", flush=True)
        try:
            http.serve_forever()
        except KeyboardInterrupt:
            pass
