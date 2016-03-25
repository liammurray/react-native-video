'use strict';

import React, {
  AppRegistry,
  Component,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

import Video from 'react-native-video';
import AndroidUtil from './AndroidUtil';
import Orientation from 'react-native-orientation';

const fullScreen = false;

class VideoPlayer extends Component {
  constructor(props) {
    super(props);
    this.onLoad = this.onLoad.bind(this);
    this.onProgress = this.onProgress.bind(this);
    this.onEnterFullScreen = this.onEnterFullScreen.bind(this);
    this.onExitFullScreen = this.onExitFullScreen.bind(this);
    AndroidUtil.setFullScreenMode(fullScreen);
  }

  state = {
    rate: 1,
    volume: 1,
    muted: false,
    resizeMode: 'contain',
    duration: 0.0,
    currentTime: 0.0,
    controls:true,
    autoHideNav:fullScreen
  };

  componentDidMount() {
    //Orientation.lockToPortrait();
    //Orientation.lockToLandscape();
    //Orientation.unlockAllOrientations(); 
    //Orientation.addOrientationListener(this._orientationDidChange);
  }

  onLoad(data) {
    this.setState({duration: data.duration});
  }

  onEnterFullScreen(data) {
    console.log("onEnterFullScreen")
    AndroidUtil.setFullScreenMode(true);
    Orientation.unlockAllOrientations(); 
    this.setState({autoHideNav: true});
  }

  onExitFullScreen(data) {
    console.log("onExitFullScreen")
    Orientation.lockToPortrait();
    AndroidUtil.setFullScreenMode(false);
    this.setState({autoHideNav: false});
  }


  onProgress(data) {
    if (!this.state.controls) {
      this.setState({currentTime: data.currentTime});
    }
  }

  getCurrentTimePercentage() {
    if (this.state.currentTime > 0) {
      return parseFloat(this.state.currentTime) / parseFloat(this.state.duration);
    } else {
      return 0;
    }
  }

  renderRateControl(rate) {
    const isSelected = (this.state.rate == rate);

    return (
      <TouchableOpacity onPress={() => { this.setState({rate: rate}) }}>
        <Text style={[styles.controlOption, {fontWeight: isSelected ? "bold" : "normal"}]}>
          {rate}
        </Text>
      </TouchableOpacity>
    )
  }

  renderResizeModeControl(resizeMode) {
    const isSelected = (this.state.resizeMode == resizeMode);

    return (
      <TouchableOpacity onPress={() => { this.setState({resizeMode: resizeMode}) }}>
        <Text style={[styles.controlOption, {fontWeight: isSelected ? "bold" : "normal"}]}>
          {resizeMode}
        </Text>
      </TouchableOpacity>
    )
  }

  renderVolumeControl(volume) {
    const isSelected = (this.state.volume == volume);

    return (
      <TouchableOpacity onPress={() => { this.setState({volume: volume}) }}>
        <Text style={[styles.controlOption, {fontWeight: isSelected ? "bold" : "normal"}]}>
          {volume * 100}%
        </Text>
      </TouchableOpacity>
    )
  }

  renderExtraControls() {
     return (
      <View style={styles.controls}>
      <View style={styles.generalControls}>
            <View style={styles.rateControl}>
              {this.renderRateControl(0.25)}
              {this.renderRateControl(0.5)}
              {this.renderRateControl(1.0)}
              {this.renderRateControl(1.5)}
              {this.renderRateControl(2.0)}
            </View>

            <View style={styles.volumeControl}>
              {this.renderVolumeControl(0.5)}
              {this.renderVolumeControl(1)}
              {this.renderVolumeControl(1.5)}
            </View>

            <View style={styles.resizeModeControl}>
              {this.renderResizeModeControl('cover')}
              {this.renderResizeModeControl('contain')}
              {this.renderResizeModeControl('stretch')}
            </View>
          </View>
          </View>)
  }

  render() {
    var video = (
      <Video source={{uri: "assets-library:///broadchurch"}}
                 style={styles.fullScreen}
                 rate={this.state.rate}
                 paused={this.state.paused}
                 volume={this.state.volume}
                 muted={this.state.muted}
                 resizeMode={this.state.resizeMode}
                 onLoad={this.onLoad}
                 onEnterFullScreen={this.onEnterFullScreen}
                 onExitFullScreen={this.onExitFullScreen}
                 controls={this.state.controls}
                 autoHideNav={this.state.autoHideNav}
                 onProgress={this.onProgress}
                 onEnd={() => {}}
                 repeat={false} />
      );

   var extraControls = this.renderExtraControls();
   return (<View style={styles.container}>{video}{extraControls}</View>);
  
  }
}


const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'black',
  },
  fullScreen: {
    position: 'absolute',
    top: 0,
    left: 0,
    bottom: 0,
    right: 0,
  },
  controls: {
    backgroundColor: "transparent",
    borderRadius: 5,
    position: 'absolute',
    top: 40,
    left: 20,
    right: 20,
  },
  progress: {
    flex: 1,
    flexDirection: 'row',
    borderRadius: 3,
    overflow: 'hidden',
  },
  innerProgressCompleted: {
    height: 20,
    backgroundColor: '#cccccc',
  },
  innerProgressRemaining: {
    height: 20,
    backgroundColor: '#2C2C2C',
  },
  generalControls: {
    flex: 1,
    flexDirection: 'row',
    borderRadius: 4,
    overflow: 'hidden',
    paddingBottom: 10,
  },
  rateControl: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  volumeControl: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  resizeModeControl: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  controlOption: {
    alignSelf: 'center',
    fontSize: 11,
    color: "white",
    paddingLeft: 2,
    paddingRight: 2,
    lineHeight: 12,
  },
});

AppRegistry.registerComponent('VideoPlayer', () => VideoPlayer);
