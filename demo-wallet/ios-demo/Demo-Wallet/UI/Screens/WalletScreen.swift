//
//  WalletScreen.swift
//  Demo-Wallet
//
//  Created by Andrew Carlson on 7/5/21.
//

import Foundation
import SwiftUI
import DemoWalletKotlin

struct WalletScreen: View {
    @EnvironmentObject private var observable: SystemObservable
    @EnvironmentObject private var router: Router
    
    var body: some View {
        return AnyView(VStack(alignment:.leading) {
            Text("Wallets")
                .fontWeight(.bold)
                .frame(maxWidth:.infinity, alignment: .center)
                .padding(4)
            
            if (observable.wallets.isEmpty) {
                HStack(alignment: .center) {
                    ActivityIndicator(shouldAnimate: Binding.constant(true))
                    Text("Loading...")
                }.frame(maxWidth: .infinity, alignment: .center)
            }
            
            List(Array(zip(observable.wallets, observable.syncStates)), id: \.0) { wallet, syncing in
                WalletItemView(wallet: wallet, syncing: syncing)
                    .onTapGesture {
                        router.pushRoute(Route.ViewWallet(wallet: wallet))
                    }
            }
        }.padding(4))
    }
}

