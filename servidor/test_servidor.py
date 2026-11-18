import http.client
import json
import sqlite3
import tempfile
import threading
import unittest
from contextlib import closing
from http.server import ThreadingHTTPServer
from pathlib import Path

from servidor import Servicio


class SincronizacionTest(unittest.TestCase):
    def test_guardar_y_consultar_con_el_mismo_cliente(self):
        dato = {"id": 42, "titulo": "Estudiar Room", "categoria": "estudio",
                "prioridad": "media", "estado": "pendiente", "fecha": 1000}
        with tempfile.TemporaryDirectory() as carpeta, ThreadingHTTPServer(("127.0.0.1", 0), Servicio) as servidor:
            servidor.archivo = str(Path(carpeta) / "prueba.sqlite3")
            with closing(sqlite3.connect(servidor.archivo)) as db, db:
                db.execute("CREATE TABLE recordatorios(id INTEGER PRIMARY KEY, contenido TEXT NOT NULL)")
            hilo = threading.Thread(target=servidor.serve_forever, daemon=True)
            hilo.start()
            try:
                with closing(http.client.HTTPConnection(*servidor.server_address, timeout=3)) as cliente:
                    cliente.request("PUT", "/recordatorios/42", json.dumps(dato), {"Content-Type": "application/json"})
                    respuesta = cliente.getresponse()
                    self.assertEqual(respuesta.status, 200)
                    self.assertEqual(respuesta.getheader("Connection"), "close")
                    guardado = json.loads(respuesta.read())
                    cliente.request("GET", "/recordatorios")
                    respuesta = cliente.getresponse()
                    self.assertEqual(respuesta.status, 200)
                    self.assertEqual(json.loads(respuesta.read()), [guardado])
            finally:
                servidor.shutdown()
                hilo.join()


if __name__ == "__main__":
    unittest.main()
