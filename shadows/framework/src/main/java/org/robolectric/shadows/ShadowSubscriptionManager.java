package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.O_MR1;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
import static android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX;

import android.os.Build.VERSION;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.robolectric.annotation.HiddenApi;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers;

@Implements(value = SubscriptionManager.class, minSdk = LOLLIPOP_MR1)
public class ShadowSubscriptionManager {

  private static boolean readPhoneStatePermission = true;
  private static boolean readPhoneNumbersPermission = true;
  public static final int INVALID_PHONE_INDEX =
      ReflectionHelpers.getStaticField(SubscriptionManager.class, "INVALID_PHONE_INDEX");

  private static int activeDataSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
  private static int defaultSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
  private static int defaultDataSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
  private static int defaultSmsSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
  private static int defaultVoiceSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

  private static final Map<Integer, String> phoneNumberMap = new HashMap<>();

  /** Returns value set with {@link #setActiveDataSubscriptionId(int)}. */
  @Implementation(minSdk = R)
  protected static int getActiveDataSubscriptionId() {
    return activeDataSubscriptionId;
  }

  /** Returns value set with {@link #setDefaultSubscriptionId(int)}. */
  @Implementation(minSdk = N)
  protected static int getDefaultSubscriptionId() {
    return defaultSubscriptionId;
  }

  /** Returns value set with {@link #setDefaultDataSubscriptionId(int)}. */
  @Implementation(minSdk = N)
  protected static int getDefaultDataSubscriptionId() {
    return defaultDataSubscriptionId;
  }

  /** Returns value set with {@link #setDefaultSmsSubscriptionId(int)}. */
  @Implementation(minSdk = N)
  protected static int getDefaultSmsSubscriptionId() {
    return defaultSmsSubscriptionId;
  }

  /** Returns value set with {@link #setDefaultVoiceSubscriptionId(int)}. */
  @Implementation(minSdk = N)
  protected static int getDefaultVoiceSubscriptionId() {
    return defaultVoiceSubscriptionId;
  }

  @Implementation(maxSdk = M)
  @HiddenApi
  protected static int getDefaultSubId() {
    return defaultSubscriptionId;
  }

  @Implementation(maxSdk = M)
  @HiddenApi
  protected static int getDefaultVoiceSubId() {
    return defaultVoiceSubscriptionId;
  }

  @Implementation(maxSdk = M)
  @HiddenApi
  protected static int getDefaultSmsSubId() {
    return defaultSmsSubscriptionId;
  }

  @Implementation(maxSdk = M)
  @HiddenApi
  protected static int getDefaultDataSubId() {
    return defaultDataSubscriptionId;
  }

  /** Sets the value that will be returned by {@link #getActiveDataSubscriptionId()}. */
  public static void setActiveDataSubscriptionId(int activeDataSubscriptionId) {
    ShadowSubscriptionManager.activeDataSubscriptionId = activeDataSubscriptionId;
  }

  /** Sets the value that will be returned by {@link #getDefaultSubscriptionId()}. */
  public static void setDefaultSubscriptionId(int defaultSubscriptionId) {
    ShadowSubscriptionManager.defaultSubscriptionId = defaultSubscriptionId;
  }

  public static void setDefaultDataSubscriptionId(int defaultDataSubscriptionId) {
    ShadowSubscriptionManager.defaultDataSubscriptionId = defaultDataSubscriptionId;
  }

  public static void setDefaultSmsSubscriptionId(int defaultSmsSubscriptionId) {
    ShadowSubscriptionManager.defaultSmsSubscriptionId = defaultSmsSubscriptionId;
  }

  public static void setDefaultVoiceSubscriptionId(int defaultVoiceSubscriptionId) {
    ShadowSubscriptionManager.defaultVoiceSubscriptionId = defaultVoiceSubscriptionId;
  }

  /**
   * Cache of phone IDs used by {@link getPhoneId}. Managed by {@link putPhoneId} and {@link
   * removePhoneId}.
   */
  private static Map<Integer, Integer> phoneIds = new HashMap<>();

  /**
   * Cache of {@link SubscriptionInfo} used by {@link #getActiveSubscriptionInfoList}. Managed by
   * {@link #setActiveSubscriptionInfoList}. May be {@code null}.
   */
  private static List<SubscriptionInfo> subscriptionList = new ArrayList<>();

  /**
   * Cache of {@link SubscriptionInfo} used by {@link #getAccessibleSubscriptionInfoList}. Managed
   * by {@link #setAccessibleSubscriptionInfos}. May be {@code null}.
   */
  private List<SubscriptionInfo> accessibleSubscriptionList = new ArrayList<>();

  /**
   * Cache of {@link SubscriptionInfo} used by {@link #getAvailableSubscriptionInfoList}. Managed by
   * {@link #setAvailableSubscriptionInfos}. May be {@code null}.
   */
  private List<SubscriptionInfo> availableSubscriptionList = new ArrayList<>();

  /**
   * List of listeners to be notified if the list of {@link SubscriptionInfo} changes. Managed by
   * {@link #addOnSubscriptionsChangedListener} and {@link removeOnSubscriptionsChangedListener}.
   */
  private List<OnSubscriptionsChangedListener> listeners = new ArrayList<>();

  /**
   * Cache of subscription ids used by {@link #isNetworkRoaming}. Managed by {@link
   * #setNetworkRoamingStatus} and {@link #clearNetworkRoamingStatus}.
   */
  private Set<Integer> roamingSimSubscriptionIds = new HashSet<>();

  /**
   * Returns the active list of {@link SubscriptionInfo} that were set via {@link
   * #setActiveSubscriptionInfoList}.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  protected List<SubscriptionInfo> getActiveSubscriptionInfoList() {
    checkReadPhoneStatePermission();
    return subscriptionList;
  }

  /**
   * Returns the accessible list of {@link SubscriptionInfo} that were set via {@link
   * #setAccessibleSubscriptionInfoList}.
   */
  @Implementation(minSdk = O_MR1)
  protected List<SubscriptionInfo> getAccessibleSubscriptionInfoList() {
    return accessibleSubscriptionList;
  }

  /**
   * Returns the available list of {@link SubscriptionInfo} that were set via {@link
   * #setAvailableSubscriptionInfoList}.
   */
  @Implementation(minSdk = O_MR1)
  protected List<SubscriptionInfo> getAvailableSubscriptionInfoList() {
    return availableSubscriptionList;
  }

  /**
   * Returns the size of the list of {@link SubscriptionInfo} that were set via {@link
   * #setActiveSubscriptionInfoList}. If no list was set, returns 0.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  protected int getActiveSubscriptionInfoCount() {
    checkReadPhoneStatePermission();
    return subscriptionList == null ? 0 : subscriptionList.size();
  }

  /**
   * Returns subscription that were set via {@link #setActiveSubscriptionInfoList} if it can find
   * one with the specified id or null if none found.
   *
   * <p>An exception will be thrown if the READ_PHONE_STATE permission has not been granted.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  protected SubscriptionInfo getActiveSubscriptionInfo(int subId) {
    checkReadPhoneStatePermission();
    if (subscriptionList == null) {
      return null;
    }
    for (SubscriptionInfo info : subscriptionList) {
      if (info.getSubscriptionId() == subId) {
        return info;
      }
    }
    return null;
  }

  /**
   * @return the maximum number of active subscriptions that will be returned by {@link
   *     #getActiveSubscriptionInfoList} and the value returned by {@link
   *     #getActiveSubscriptionInfoCount}.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  protected int getActiveSubscriptionInfoCountMax() {
    List<SubscriptionInfo> infoList = getActiveSubscriptionInfoList();

    if (infoList == null) {
      return getActiveSubscriptionInfoCount();
    }

    return Math.max(getActiveSubscriptionInfoList().size(), getActiveSubscriptionInfoCount());
  }

  /**
   * Returns subscription that were set via {@link #setActiveSubscriptionInfoList} if it can find
   * one with the specified slot index or null if none found.
   */
  @Implementation(minSdk = N)
  protected SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex) {
    checkReadPhoneStatePermission();
    if (subscriptionList == null) {
      return null;
    }
    for (SubscriptionInfo info : subscriptionList) {
      if (info.getSimSlotIndex() == slotIndex) {
        return info;
      }
    }
    return null;
  }

  /**
   * Sets the active list of {@link SubscriptionInfo}. This call internally triggers {@link
   * OnSubscriptionsChangedListener#onSubscriptionsChanged()} to all the listeners.
   *
   * <p>"Active" here means subscriptions which are currently mapped to a live modem stack in the
   * device (i.e. the modem will attempt to use them to connect to nearby towers), and they are
   * expected to have {@link SubscriptionInfo#getSimSlotIndex()} >= 0. A subscription being "active"
   * in the device does NOT have any relation to a carrier's "activation" process for subscribers'
   * SIMs.
   *
   * @param list - The subscription info list, can be null.
   */
  public void setActiveSubscriptionInfoList(List<SubscriptionInfo> list) {
    subscriptionList = list;
    dispatchOnSubscriptionsChanged();
  }

  /**
   * Sets the accessible list of {@link SubscriptionInfo}. This call internally triggers {@link
   * OnSubscriptionsChangedListener#onSubscriptionsChanged()} to all the listeners.
   *
   * <p>"Accessible" here means subscriptions which are eSIM ({@link SubscriptionInfo#isEmbedded})
   * and "owned" by the calling app, i.e. by {@link
   * SubscriptionManager#canManageSubscription(SubscriptionInfo)}. They may be active, or
   * installed-but-inactive. This is generally intended to be called by carrier apps that directly
   * manage their own eSIM profiles on the device in concert with {@link
   * android.telephony.EuiccManager}.
   *
   * @param list - The subscription info list, can be null.
   */
  public void setAccessibleSubscriptionInfoList(List<SubscriptionInfo> list) {
    accessibleSubscriptionList = list;
    dispatchOnSubscriptionsChanged();
  }

  /**
   * Sets the available list of {@link SubscriptionInfo}. This call internally triggers {@link
   * OnSubscriptionsChangedListener#onSubscriptionsChanged()} to all the listeners.
   *
   * <p>"Available" here means all active subscriptions (see {@link #setActiveSubscriptionInfoList})
   * combined with all installed-but-inactive eSIM subscriptions (similar to {@link
   * #setAccessibleSubscriptionInfoList}, but not filtered to one particular app's "ownership"
   * rights for subscriptions). This is generally intended to be called by system components such as
   * the eSIM LPA or Settings that allow the user to manage all subscriptions on the device through
   * some system-provided user interface.
   *
   * @param list - The subscription info list, can be null.
   */
  public void setAvailableSubscriptionInfoList(List<SubscriptionInfo> list) {
    availableSubscriptionList = list;
    dispatchOnSubscriptionsChanged();
  }

  /**
   * Sets the active list of {@link SubscriptionInfo}. This call internally triggers {@link
   * OnSubscriptionsChangedListener#onSubscriptionsChanged()} to all the listeners.
   */
  public void setActiveSubscriptionInfos(SubscriptionInfo... infos) {
    if (infos == null) {
      setActiveSubscriptionInfoList(ImmutableList.of());
    } else {
      setActiveSubscriptionInfoList(Arrays.asList(infos));
    }
  }

  /**
   * Sets the accessible list of {@link SubscriptionInfo}. This call internally triggers {@link
   * OnSubscriptionsChangedListener#onSubscriptionsChanged()} to all the listeners.
   */
  public void setAccessibleSubscriptionInfos(SubscriptionInfo... infos) {
    if (infos == null) {
      setAccessibleSubscriptionInfoList(ImmutableList.of());
    } else {
      setAccessibleSubscriptionInfoList(Arrays.asList(infos));
    }
  }

  /**
   * Sets the available list of {@link SubscriptionInfo}. This call internally triggers {@link
   * OnSubscriptionsChangedListener#onSubscriptionsChanged()} to all the listeners.
   */
  public void setAvailableSubscriptionInfos(SubscriptionInfo... infos) {
    if (infos == null) {
      setAvailableSubscriptionInfoList(ImmutableList.of());
    } else {
      setAvailableSubscriptionInfoList(Arrays.asList(infos));
    }
  }

  /**
   * Adds a listener to a local list of listeners. Will be triggered by {@link
   * #setActiveSubscriptionInfoList} when the local list of {@link SubscriptionInfo} is updated.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  protected void addOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
    listeners.add(listener);
    listener.onSubscriptionsChanged();
  }

  /**
   * Adds a listener to a local list of listeners. Will be triggered by {@link
   * #setActiveSubscriptionInfoList} when the local list of {@link SubscriptionInfo} is updated.
   */
  @Implementation(minSdk = R)
  protected void addOnSubscriptionsChangedListener(
      Executor executor, OnSubscriptionsChangedListener listener) {
    listeners.add(listener);
    listener.onSubscriptionsChanged();
  }

  /**
   * Removes a listener from a local list of listeners. Will be triggered by {@link
   * #setActiveSubscriptionInfoList} when the local list of {@link SubscriptionInfo} is updated.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  protected void removeOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
    listeners.remove(listener);
  }

  /**
   * Check if a listener exists in the {@link ShadowSubscriptionManager.listeners}.
   *
   * @param listener The listener to check.
   * @return boolean True if the listener already added, otherwise false.
   */
  public boolean hasOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
    return listeners.contains(listener);
  }

  /** Returns subscription Ids that were set via {@link #setActiveSubscriptionInfoList}. */
  @Implementation(minSdk = LOLLIPOP_MR1)
  @HiddenApi
  protected int[] getActiveSubscriptionIdList() {
    final List<SubscriptionInfo> infos = getActiveSubscriptionInfoList();
    if (infos == null) {
      return new int[0];
    }
    int[] ids = new int[infos.size()];
    for (int i = 0; i < infos.size(); i++) {
      ids[i] = infos.get(i).getSubscriptionId();
    }
    return ids;
  }

  /**
   * Notifies {@link OnSubscriptionsChangedListener} listeners that the list of {@link
   * SubscriptionInfo} has been updated.
   */
  private void dispatchOnSubscriptionsChanged() {
    for (OnSubscriptionsChangedListener listener : listeners) {
      listener.onSubscriptionsChanged();
    }
  }

  /** Clears the local cache of roaming subscription Ids used by {@link #isNetworkRoaming}. */
  public void clearNetworkRoamingStatus() {
    roamingSimSubscriptionIds.clear();
  }

  /**
   * If isNetworkRoaming is set, it will mark the provided sim subscriptionId as roaming in a local
   * cache. If isNetworkRoaming is unset it will remove the subscriptionId from the local cache. The
   * local cache is used to provide roaming status returned by {@link #isNetworkRoaming}.
   */
  public void setNetworkRoamingStatus(int simSubscriptionId, boolean isNetworkRoaming) {
    if (isNetworkRoaming) {
      roamingSimSubscriptionIds.add(simSubscriptionId);
    } else {
      roamingSimSubscriptionIds.remove(simSubscriptionId);
    }
  }

  /**
   * Uses the local cache of roaming sim subscription Ids managed by {@link
   * #setNetworkRoamingStatus} to return subscription Ids marked as roaming. Otherwise subscription
   * Ids will be considered as non-roaming if they are not in the cache.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  protected boolean isNetworkRoaming(int simSubscriptionId) {
    return roamingSimSubscriptionIds.contains(simSubscriptionId);
  }

  /** Adds a subscription ID-phone ID mapping to the map used by {@link getPhoneId}. */
  public static void putPhoneId(int subId, int phoneId) {
    phoneIds.put(subId, phoneId);
  }

  /**
   * Removes a subscription ID-phone ID mapping from the map used by {@link getPhoneId}.
   *
   * @return the previous phone ID associated with the subscription ID, or null if there was no
   *     mapping for the subscription ID
   */
  public static Integer removePhoneId(int subId) {
    return phoneIds.remove(subId);
  }

  /**
   * Removes all mappings between subscription IDs and phone IDs from the map used by {@link
   * getPhoneId}.
   */
  public static void clearPhoneIds() {
    phoneIds.clear();
  }

  /**
   * Uses the map of subscription IDs to phone IDs managed by {@link putPhoneId} and {@link
   * removePhoneId} to return the phone ID for a given subscription ID.
   */
  @Implementation(minSdk = LOLLIPOP_MR1, maxSdk = P)
  @HiddenApi
  protected static int getPhoneId(int subId) {
    if (phoneIds.containsKey(subId)) {
      return phoneIds.get(subId);
    }
    return INVALID_PHONE_INDEX;
  }

  /**
   * Older form of {@link #getSubscriptionId} that was designed prior to mainstream multi-SIM
   * support, so its {@code int[]} return type ended up being an unused vestige from that older
   * design.
   */
  @Implementation(minSdk = LOLLIPOP_MR1)
  @HiddenApi
  protected static int[] getSubId(int slotIndex) {
    int subId = getSubscriptionId(slotIndex);
    return subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ? null : new int[] {subId};
  }

  /**
   * Older form of {@link #getSubscriptionId} that was designed prior to mainstream multi-SIM
   * support, so its {@code int[]} return type ended up being an unused vestige from that older
   * design.
   */
  @Implementation(minSdk = Q)
  protected int[] getSubscriptionIds(int slotIndex) {
    return getSubId(slotIndex);
  }

  /**
   * Derives the subscription ID corresponding to an "active" {@link SubscriptionInfo} for the given
   * SIM slot index.
   */
  @Implementation(minSdk = UPSIDE_DOWN_CAKE)
  protected static int getSubscriptionId(int slotIndex) {
    // Intentionally not re-calling getActiveSubscriptionInfoForSimSlotIndex since this API does not
    // require any permissions (and this is static).
    if (subscriptionList == null) {
      return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }
    for (SubscriptionInfo info : subscriptionList) {
      if (info.getSimSlotIndex() == slotIndex) {
        return info.getSubscriptionId();
      }
    }
    return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
  }

  @Implementation(minSdk = O)
  protected static int getSlotIndex(int subscriptionId) {
    if (subscriptionList != null) {
      for (SubscriptionInfo info : subscriptionList) {
        if (info.getSubscriptionId() == subscriptionId) {
          return info.getSimSlotIndex();
        }
      }
    }
    return INVALID_SIM_SLOT_INDEX;
  }

  /**
   * When set to false methods requiring {@link android.Manifest.permission.READ_PHONE_STATE}
   * permission will throw a {@link SecurityException}. By default it's set to true for backwards
   * compatibility.
   */
  public void setReadPhoneStatePermission(boolean readPhoneStatePermission) {
    this.readPhoneStatePermission = readPhoneStatePermission;
  }

  private void checkReadPhoneStatePermission() {
    if (!readPhoneStatePermission) {
      throw new SecurityException();
    }
  }

  /**
   * When set to false methods requiring {@link android.Manifest.permission.READ_PHONE_NUMBERS}
   * permission will throw a {@link SecurityException}. By default it's set to true for backwards
   * compatibility.
   */
  public void setReadPhoneNumbersPermission(boolean readPhoneNumbersPermission) {
    this.readPhoneNumbersPermission = readPhoneNumbersPermission;
  }

  private void checkReadPhoneNumbersPermission() {
    if (!readPhoneNumbersPermission) {
      throw new SecurityException();
    }
  }

  /**
   * Returns the phone number for the given {@code subscriptionId}, or an empty string if not
   * available.
   *
   * <p>The phone number can be set by {@link #setPhoneNumber(int, String)}
   *
   * <p>An exception will be thrown if the READ_PHONE_NUMBERS permission has not been granted.
   */
  @Implementation(minSdk = TIRAMISU)
  protected String getPhoneNumber(int subscriptionId) {
    checkReadPhoneNumbersPermission();
    return phoneNumberMap.getOrDefault(subscriptionId, "");
  }

  /**
   * Returns the phone number for the given {@code subscriptionId}, or an empty string if not
   * available. {@code source} is ignored and will return the same as {@link #getPhoneNumber(int)}.
   *
   * <p>The phone number can be set by {@link #setPhoneNumber(int, String)}
   */
  @Implementation(minSdk = TIRAMISU)
  protected String getPhoneNumber(int subscriptionId, int source) {
    return getPhoneNumber(subscriptionId);
  }

  /** Sets the phone number returned by {@link #getPhoneNumber(int)}. */
  public void setPhoneNumber(int subscriptionId, String phoneNumber) {
    phoneNumberMap.put(subscriptionId, phoneNumber);
  }

  @Resetter
  public static void reset() {
    activeDataSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    defaultDataSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    defaultSmsSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    defaultVoiceSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    defaultSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    subscriptionList = new ArrayList<>();
    phoneIds.clear();
    phoneNumberMap.clear();
    readPhoneStatePermission = true;
    readPhoneNumbersPermission = true;
  }

  /** Builder class to create instance of {@link SubscriptionInfo}. */
  public static class SubscriptionInfoBuilder {
    private final SubscriptionInfo subscriptionInfo =
        ReflectionHelpers.callConstructor(SubscriptionInfo.class);

    public static SubscriptionInfoBuilder newBuilder() {
      return new SubscriptionInfoBuilder();
    }

    public SubscriptionInfo buildSubscriptionInfo() {
      return subscriptionInfo;
    }

    public SubscriptionInfoBuilder setId(int id) {
      ReflectionHelpers.setField(subscriptionInfo, "mId", id);
      return this;
    }

    public SubscriptionInfoBuilder setIccId(String iccId) {
      ReflectionHelpers.setField(subscriptionInfo, "mIccId", iccId);
      return this;
    }

    public SubscriptionInfoBuilder setSimSlotIndex(int index) {
      ReflectionHelpers.setField(subscriptionInfo, "mSimSlotIndex", index);
      return this;
    }

    public SubscriptionInfoBuilder setDisplayName(String name) {
      ReflectionHelpers.setField(subscriptionInfo, "mDisplayName", name);
      return this;
    }

    public SubscriptionInfoBuilder setCarrierName(String carrierName) {
      ReflectionHelpers.setField(subscriptionInfo, "mCarrierName", carrierName);
      return this;
    }

    public SubscriptionInfoBuilder setIconTint(int iconTint) {
      ReflectionHelpers.setField(subscriptionInfo, "mIconTint", iconTint);
      return this;
    }

    public SubscriptionInfoBuilder setNumber(String number) {
      ReflectionHelpers.setField(subscriptionInfo, "mNumber", number);
      return this;
    }

    public SubscriptionInfoBuilder setDataRoaming(int dataRoaming) {
      ReflectionHelpers.setField(subscriptionInfo, "mDataRoaming", dataRoaming);
      return this;
    }

    public SubscriptionInfoBuilder setCountryIso(String countryIso) {
      ReflectionHelpers.setField(subscriptionInfo, "mCountryIso", countryIso);
      return this;
    }

    public SubscriptionInfoBuilder setProfileClass(int profileClass) {
      ReflectionHelpers.setField(subscriptionInfo, "mProfileClass", profileClass);
      return this;
    }

    public SubscriptionInfoBuilder setIsEmbedded(boolean isEmbedded) {
      ReflectionHelpers.setField(subscriptionInfo, "mIsEmbedded", isEmbedded);
      return this;
    }

    public SubscriptionInfoBuilder setIsOpportunistic(boolean isOpportunistic) {
      ReflectionHelpers.setField(subscriptionInfo, "mIsOpportunistic", isOpportunistic);
      return this;
    }

    public SubscriptionInfoBuilder setMnc(String mnc) {
      if (VERSION.SDK_INT < Q) {
        ReflectionHelpers.setField(subscriptionInfo, "mMnc", Integer.valueOf(mnc));
      } else {
        ReflectionHelpers.setField(subscriptionInfo, "mMnc", mnc);
      }
      return this;
    }

    public SubscriptionInfoBuilder setMcc(String mcc) {
      if (VERSION.SDK_INT < Q) {
        ReflectionHelpers.setField(subscriptionInfo, "mMcc", Integer.valueOf(mcc));
      } else {
        ReflectionHelpers.setField(subscriptionInfo, "mMcc", mcc);
      }
      return this;
    }

    // Use {@link #newBuilder} to construct builders.
    private SubscriptionInfoBuilder() {}
  }
}
