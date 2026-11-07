
import React, { useState } from 'react';
import * as DocumentPicker from 'expo-document-picker';
import {
  View,
  Text,
  StyleSheet,
  StatusBar,
  TouchableOpacity,
  ScrollView,
  SafeAreaView,
  Alert,
} from 'react-native';

const servers = [
  { id: 'de', name: 'آلمان', city: 'فرانکفورت', flag: '🇩🇪', ping: 'تنظیم نشده' },
  { id: 'nl', name: 'هلند', city: 'آمستردام', flag: '🇳🇱', ping: 'تنظیم نشده' },
  { id: 'fi', name: 'فنلاند', city: 'هلسینکی', flag: '🇫🇮', ping: 'تنظیم نشده' },
];

export default function App() {
  const connected = false;
  const [selected, setSelected] = useState('de');
  const [configName, setConfigName] = useState<string | null>(null);

  const server = servers.find(s => s.id === selected)!;

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" />

      <ScrollView contentContainerStyle={styles.container}>

        <View style={styles.header}>
          <View>
            <Text style={styles.logo}>MORZEX<Text style={styles.logoAccent}> VPN</Text></Text>
            <Text style={styles.subtitle}>SECURE CONNECTION</Text>
          </View>

          <View style={styles.badge}>
            <View style={[styles.dot, connected && styles.green]} />
            <Text style={styles.badgeText}>
              {connected ? 'CONNECTED' : 'NOT CONFIGURED'}
            </Text>
          </View>
        </View>

        <View style={styles.hero}>
          <Text style={styles.eyebrow}>YOUR PRIVACY MATTERS</Text>
          <Text style={styles.title}>اتصال امن،{'\n'}تجربه آزاد</Text>
          <Text style={styles.description}>
            اتصال خود را مدیریت کن و وضعیت سرویس را مشاهده کن.
          </Text>

          <TouchableOpacity
            activeOpacity={0.8}
            style={[
              styles.connectButton,
              connected && styles.connectedButton
            ]}
            onPress={() => Alert.alert('سرور تنظیم نشده', 'برای اتصال واقعی، ابتدا باید کانفیگ معتبر WireGuard وارد شود.')}
          >
            <Text style={styles.power}>⏻</Text>
            <Text style={styles.connectText}>
              {connected ? 'قطع اتصال' : 'اتصال'}
            </Text>
            <Text style={styles.connectSub}>
              {connected ? 'CONNECTED' : 'SERVER NOT CONFIGURED'}
            </Text>
          </TouchableOpacity>

          <Text style={styles.status}>
            {connected ? '● متصل' : '○ سرور WireGuard تنظیم نشده است'}
          </Text>
        </View>

        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>انتخاب سرور</Text>
          <Text style={styles.sectionHint}>SERVERS</Text>
        </View>

        {servers.map(item => (
          <TouchableOpacity
            key={item.id}
            activeOpacity={0.8}
            style={[
              styles.server,
              selected === item.id && styles.serverSelected
            ]}
            onPress={() => setSelected(item.id)}
          >
            <Text style={styles.flag}>{item.flag}</Text>

            <View style={styles.serverInfo}>
              <Text style={styles.serverName}>{item.name}</Text>
              <Text style={styles.serverCity}>{item.city}</Text>
            </View>

            <Text style={styles.ping}>{item.ping}</Text>

            <View style={[
              styles.radio,
              selected === item.id && styles.radioActive
            ]}>
              {selected === item.id && <View style={styles.radioInner} />}
            </View>
          </TouchableOpacity>
        ))}

        <View style={styles.configBox}>
          <Text style={styles.configTitle}>WIREGUARD CONFIGURATION</Text>

          <TouchableOpacity
            style={styles.configButton}
            activeOpacity={0.8}
            onPress={async () => {
              try {
                const result = await DocumentPicker.getDocumentAsync({
                  type: '*/*',
                  copyToCacheDirectory: true,
                });

                if (!result.canceled && result.assets.length > 0) {
                  const name = result.assets[0].name;

                  if (!name.toLowerCase().endsWith('.conf')) {
                    Alert.alert('فایل نامعتبر', 'لطفاً یک فایل با پسوند .conf انتخاب کن.');
                    return;
                  }

                  setConfigName(name);
                  Alert.alert('فایل انتخاب شد', 'نام فایل: ' + name);
                }
              } catch {
                Alert.alert('خطا', 'انتخاب فایل انجام نشد.');
              }
            }}
          >
            <Text style={styles.configButtonText}>＋  وارد کردن کانفیگ WireGuard</Text>
          </TouchableOpacity>

          <Text style={styles.configName}>
            {configName ? 'فایل انتخاب‌شده: ' + configName : 'هنوز فایلی انتخاب نشده است'}
          </Text>

          <Text style={styles.configHint}>
            انتخاب فایل به‌تنهایی اتصال VPN را فعال نمی‌کند.
          </Text>
        </View>

        <View style={styles.stats}>
          <View style={styles.statBox}>
            <Text style={styles.statIcon}>↓</Text>
            <Text style={styles.statValue}>0 MB</Text>
            <Text style={styles.statLabel}>DOWNLOAD</Text>
          </View>

          <View style={styles.statDivider} />

          <View style={styles.statBox}>
            <Text style={styles.statIcon}>↑</Text>
            <Text style={styles.statValue}>0 MB</Text>
            <Text style={styles.statLabel}>UPLOAD</Text>
          </View>
        </View>

        <View style={styles.info}>
          <Text style={styles.infoTitle}>سرور انتخاب‌شده</Text>
          <Text style={styles.infoValue}>
            {server.flag}  {server.name} - {server.city}
          </Text>
          <Text style={styles.infoDescription}>
            این سرور هنوز پیکربندی نشده است و اتصال واقعی WireGuard فعال نیست.
          </Text>
        </View>

        <View style={styles.footer}>
          <Text style={styles.footerLogo}>MORZEX VPN</Text>
          <Text style={styles.footerText}>POWERED BY MORZEX</Text>
          <Text style={styles.version}>VERSION 1.0.0</Text>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#080B14',
  },
  container: {
    padding: 22,
    paddingBottom: 40,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
    marginBottom: 38,
  },
  logo: {
    color: '#FFFFFF',
    fontSize: 25,
    fontWeight: '900',
    letterSpacing: 1,
  },
  logoAccent: {
    color: '#9B7BFF',
  },
  subtitle: {
    color: '#717B91',
    fontSize: 9,
    letterSpacing: 3,
    marginTop: 5,
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#171D2B',
    padding: 10,
    borderRadius: 20,
  },
  dot: {
    width: 7,
    height: 7,
    borderRadius: 5,
    backgroundColor: '#F0A34D',
    marginRight: 7,
  },
  green: {
    backgroundColor: '#43D99A',
  },
  badgeText: {
    color: '#BFC8D9',
    fontSize: 9,
    fontWeight: 'bold',
  },
  hero: {
    alignItems: 'center',
    paddingVertical: 15,
  },
  eyebrow: {
    color: '#A78BFA',
    fontSize: 10,
    letterSpacing: 3,
    fontWeight: 'bold',
  },
  title: {
    color: '#FFFFFF',
    fontSize: 36,
    fontWeight: '900',
    textAlign: 'center',
    lineHeight: 52,
    marginTop: 17,
  },
  description: {
    color: '#8A94A8',
    fontSize: 13,
    textAlign: 'center',
    marginTop: 10,
  },
  connectButton: {
    width: 205,
    height: 205,
    borderRadius: 110,
    borderWidth: 3,
    borderColor: '#8C70FF',
    backgroundColor: '#19182F',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 35,
    elevation: 12,
    shadowColor: '#8764FF',
    shadowOpacity: 0.3,
    shadowRadius: 25,
  },
  connectedButton: {
    borderColor: '#43D99A',
    backgroundColor: '#102A29',
  },
  power: {
    color: '#B49CFF',
    fontSize: 53,
  },
  connectText: {
    color: '#FFFFFF',
    fontSize: 22,
    fontWeight: 'bold',
    marginTop: 3,
  },
  connectSub: {
    color: '#8992A9',
    fontSize: 9,
    letterSpacing: 2,
    marginTop: 7,
  },
  status: {
    color: '#9EA8BC',
    fontSize: 12,
    marginTop: 22,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 43,
    marginBottom: 15,
  },
  sectionTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 'bold',
  },
  sectionHint: {
    color: '#8F7AFF',
    fontSize: 10,
    letterSpacing: 2,
  },
  server: {
    backgroundColor: '#121827',
    borderWidth: 1,
    borderColor: '#222B3D',
    borderRadius: 15,
    padding: 15,
    marginBottom: 10,
    flexDirection: 'row',
    alignItems: 'center',
  },
  serverSelected: {
    borderColor: '#9275FF',
    backgroundColor: '#1A1931',
  },
  flag: {
    fontSize: 27,
    marginRight: 13,
  },
  serverInfo: {
    flex: 1,
  },
  serverName: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: 'bold',
  },
  serverCity: {
    color: '#8590A6',
    fontSize: 11,
    marginTop: 5,
  },
  ping: {
    color: '#52D9A1',
    fontSize: 11,
    marginRight: 13,
  },
  radio: {
    width: 17,
    height: 17,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#69748B',
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioActive: {
    borderColor: '#A78BFA',
  },
  radioInner: {
    width: 9,
    height: 9,
    borderRadius: 5,
    backgroundColor: '#A78BFA',
  },
  stats: {
    flexDirection: 'row',
    backgroundColor: '#121827',
    borderRadius: 16,
    padding: 20,
    marginTop: 23,
    alignItems: 'center',
  },
  statBox: {
    flex: 1,
    alignItems: 'center',
  },
  statIcon: {
    color: '#A78BFA',
    fontSize: 24,
  },
  statValue: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: 'bold',
    marginTop: 7,
  },
  statLabel: {
    color: '#7D889D',
    fontSize: 9,
    marginTop: 6,
    letterSpacing: 1,
  },
  statDivider: {
    width: 1,
    height: 55,
    backgroundColor: '#30384B',
  },
  configBox: {
    marginTop: 22,
    padding: 17,
    backgroundColor: '#171B2A',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#282D41',
  },
  configTitle: {
    color: '#A78BFA',
    fontSize: 11,
    fontWeight: 'bold',
    marginBottom: 13,
  },
  configButton: {
    backgroundColor: '#302454',
    borderRadius: 12,
    padding: 15,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#A78BFA',
  },
  configButtonText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: 'bold',
  },
  configName: {
    color: '#FFFFFF',
    fontSize: 11,
    marginTop: 13,
  },
  configHint: {
    color: '#818BA0',
    fontSize: 10,
    marginTop: 8,
  },
  info: {
    marginTop: 22,
    backgroundColor: '#171B2A',
    borderRadius: 14,
    padding: 17,
    borderWidth: 1,
    borderColor: '#282D41',
  },
  infoTitle: {
    color: '#A78BFA',
    fontSize: 12,
    fontWeight: 'bold',
  },
  infoValue: {
    color: '#FFFFFF',
    fontSize: 15,
    marginTop: 10,
  },
  infoDescription: {
    color: '#818BA0',
    fontSize: 11,
    marginTop: 10,
    lineHeight: 19,
  },
  footer: {
    alignItems: 'center',
    marginTop: 40,
  },
  footerLogo: {
    color: '#A78BFA',
    fontWeight: '900',
    fontSize: 17,
    letterSpacing: 3,
  },
  footerText: {
    color: '#6E7890',
    fontSize: 9,
    letterSpacing: 2,
    marginTop: 8,
  },
  version: {
    color: '#515C71',
    fontSize: 9,
    marginTop: 9,
  },
});

