package com.example.composecontactos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.composecontactos.ui.screens.ContactsScreen
import com.example.composecontactos.ui.screens.DetailScreen
import com.example.composecontactos.ui.screens.HomeScreen

@Composable
fun NavigationWrapper(){
    val navController = rememberNavController()
    // NavHost es el contenedor principal que gestiona todas las pantallas y la navegación
// navController: controla la navegación entre pantallas
// startDestination: define cuál será la primera pantalla que se muestra al iniciar la app
    NavHost(navController = navController, startDestination = Home){

        // Define la ruta para la pantalla Home (pantalla inicial)
        // composable<Home> usa type-safe navigation con la clase Home como identificador
        composable<Home>{
            HomeScreen(
                // navigateToContacts es un callback que se ejecuta cuando el usuario
                // quiere ir a la pantalla de contactos
                // navController.navigate(Contacts) agrega Contacts a la pila de navegación
                navigateToContacts = { navController.navigate(Contacts) }
            )
        }

        // Define la ruta para la pantalla de Contacts (lista de contactos)
        composable<Contacts>{
            ContactsScreen(
                // onNavigateBack permite regresar a la pantalla anterior
                // navigateUp() quita la pantalla actual de la pila y regresa a la anterior
                onNavigateBack = { navController.navigateUp() },

                // onNavigateToDetail se ejecuta cuando el usuario selecciona un contacto
                // Recibe el contacto seleccionado como parámetro
                onNavigateToDetail = { contact ->
                    // Navega a la pantalla Detail pasando los datos del contacto
                    // Se crea un objeto Detail con los datos del contacto seleccionado
                    // Estos datos viajan seguros gracias a type-safe navigation
                    navController.navigate(
                        Detail(
                            contactId = contact.id,
                            name = contact.name,
                            phoneNumber = contact.phoneNumber
                        )
                    )
                }
            )
        }

        // Define la ruta para la pantalla Detail (detalle de un contacto específico)
        // backStackEntry contiene toda la información de esta entrada en la pila de navegación
        composable<Detail>{ backStackEntry ->
            // Extrae los argumentos que fueron pasados durante la navegación
            // toRoute<Detail>() convierte los datos serializados de vuelta al objeto Detail
            val detail = backStackEntry.toRoute<Detail>()

            DetailScreen(
                // Pasa los datos individuales extraídos del objeto detail
                name = detail.name,
                phoneNumber = detail.phoneNumber,

                // Permite regresar a la pantalla de contactos
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
